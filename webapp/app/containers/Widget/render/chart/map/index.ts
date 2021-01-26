/*
 * <<
 * Davinci
 * ==
 * Copyright (C) 2016 - 2017 EDP
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * >>
 */

import { IChartProps } from '../../../components/Chart'
import { EChartOption } from 'echarts'
import echarts from 'echarts/lib/echarts'

import { decodeMetricName } from '../../../components/util'
import { getMinMaxByDataTree, getChartData } from './utils'

import { getMapOption } from './type/map'
import { getScatterOption } from './type/scatter'
import { getHeatmapOption } from './type/heatmap'
import { getLinesOption } from './type/lines'
import { getFormattedValue } from '../../../components/Config/Format'

const mapJsonCacheFlag = {} // map 地图 ID缓存

export default function (chartProps: IChartProps, drillOptions) {
  const { chartStyles, data, cols, metrics, model } = chartProps
  const { label, spec, mapItemStyle } = chartStyles
  const {
    labelColor,
    labelFontFamily,
    labelFontSize,
    labelPosition,
    showLabel
  } = label
  const itemStyle = mapItemStyle
    ? {
        normal: {
          borderType: mapItemStyle.borderType,
          areaColor: mapItemStyle.areaColor,
          borderColor: mapItemStyle.borderColor,
          borderWidth: mapItemStyle.borderWidth
        },
        emphasis: {
          areaColor: mapItemStyle.areaColorEmphasis
        }
      }
    : {}
  const { layerType, roam, linesSpeed, symbolType } = spec

  const { mapData } = drillOptions

  let dataTree = {}
  const agg = metrics[0].agg
  const metricName = decodeMetricName(metrics[0].name)
  const valueField = `${agg}(${metricName})`
  const fields = {
    provinceField: '',
    cityField: '',
    areaField: ''
  }
  cols.forEach((col) => {
    if (model[col.name].visualType === 'geoProvince') {
      fields.provinceField = col.name
    }
    if (model[col.name].visualType === 'geoCity') {
      fields.cityField = col.name
    }
  })
  dataTree = getChartData(fields, mapData, valueField, data, agg)
  const { min = 0, max = 0 } = getMinMaxByDataTree(dataTree)
  const tooltip: EChartOption.Tooltip = {
    trigger: 'item',
    formatter: (params: EChartOption.Tooltip.Format) => {
      if (!params.data) {
        return params.name
      }
      const { name, data, color } = params
      const tooltipLabels = []
      if (color) {
        tooltipLabels.push(
          `<span class="widget-tooltip-circle" style="background: ${color}"></span>`
        )
      }
      tooltipLabels.push(name)
      if (data) {
        tooltipLabels.push(': ')
        tooltipLabels.push(getFormattedValue(data.value[2], metrics[0].format))
      }
      return tooltipLabels.join('')
    }
  }

  const geo = {
    map: mapData.currentCode,
    zoom: 1,
    itemStyle,
    roam
  }
  const labelOption = {
    label: {
      normal: {
        formatter: '{b}',
        position: labelPosition,
        show: showLabel,
        color: labelColor,
        fontFamily: labelFontFamily,
        fontSize: labelFontSize
      }
    }
  }
  const baseOption = {
    tooltip,
    geo,
    labelOption,
    itemStyle,
    dataTree,
    min,
    max
  }
  let options = {}
  switch (layerType) {
    case 'map':
      options = getMapOption(chartProps, drillOptions, baseOption)
      break
    case 'scatter':
      options = getScatterOption(chartProps, drillOptions, baseOption)
      break
    case 'heatmap':
      options = getHeatmapOption(chartProps, drillOptions, baseOption)
      break
    case 'lines':
      options = getLinesOption(chartProps, drillOptions, baseOption)
      break
    default:
      throw Error('Unable to find layerType')
  }

  function getMap(codeString: string) {
    return new Promise((resolve, reject) => {
      try {
        const mapJsonFile = require(`assets/json/geoJson/${codeString}.json`)
        resolve(mapJsonFile)
      } catch (error) {
        console.log('error', error)
        reject('noMapJson')
      }
    })
  }
  // 异步加载方法
  if (mapJsonCacheFlag[mapData.currentCode]) {
    if (typeof mapJsonCacheFlag[mapData.currentCode] === 'boolean') {
      return options
    } else {
      return mapJsonCacheFlag[mapData.currentCode]
    }
  } else {
    mapJsonCacheFlag[mapData.currentCode] = getMap(mapData.currentCode).then(
      (json: object) => {
        echarts.registerMap(mapData.currentCode, json)
        mapJsonCacheFlag[mapData.currentCode] = true
        return options
      }
    )
    return mapJsonCacheFlag[mapData.currentCode]
  }

  // 同步加载方法
  // if (mapJsonCacheFlag[mapData.currentCode]) {
  //   console.log(mapData.currentCode)
  // } else {
  //   const json = require(`assets/json/geoJson/${mapData.currentCode}.json`)
  //   if (json) {
  //     mapJsonCacheFlag[mapData.currentCode] = json
  //     echarts.registerMap(mapData.currentCode, json)
  //   }
  // }
  // return options
}
