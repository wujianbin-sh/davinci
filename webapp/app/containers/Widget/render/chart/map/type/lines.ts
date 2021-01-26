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

import { IChartProps } from '../../../../components/Chart'
import { EChartOption } from 'echarts'
import geoData from 'assets/js/geo.js'
import { decodeMetricName } from '../../../../components/util'
import {
  getVisualMapOptions
} from '../utils'
import { getLegendOption, getLabelOption } from '../../util'
const provinceSuffix = ['省', '市', '特别行政区', '维吾尔自治区', '回族自治区', '壮族自治区', '自治区', '行政区']
const citySuffix = ['自治州', '市', '区', '县', '旗', '盟', '镇']
export function getLinesOption(chartProps: IChartProps, drillOptions, baseOption) {
  const { chartStyles, data, cols, metrics, model } = chartProps
  const { label, spec } = chartStyles
  const {
    layerType,
    linesSpeed,
    symbolType
  } = spec
  const dataTree = {}

  const agg = metrics[0].agg
  const metricName = decodeMetricName(metrics[0].name)

  let min = -Infinity
  let max = Infinity
  if (data.length) {
    min = Math.min(max, data[0][`${agg}(${metricName})`])
    max = Math.min(min, data[0][`${agg}(${metricName})`])
  }

  data.forEach((record) => {
    const value = record[`${agg}(${metricName})`]
    min = Math.min(min, value)
    max = Math.max(max, value)
  })
  const labelOptionLines = {
    label: getLabelOption('lines', label, metrics, true)
  }
  const getGeoCity = cols.filter((c) => model[c.name].visualType === 'geoCity')
  const getGeoProvince = cols.filter((c) => model[c.name].visualType === 'geoProvince')
  const linesSeries = []
  const legendData = []
  let effectScatterType
  let linesType
  data.forEach((d, index) => {
    let linesSeriesData = []
    let scatterData = []
    const value = d[`${agg}(${metricName})`]

    if (getGeoCity.length > 1 && d[getGeoCity[0].name] && d[getGeoCity[1].name]) {
      const fromCityInfo = getCityArea(d[getGeoCity[0].name])
      const toCityInfo = getCityArea(d[getGeoCity[1].name])

      if (fromCityInfo && toCityInfo) {
        legendData.push(d[getGeoCity[0].name])
        linesSeriesData = [{
          fromName: d[getGeoCity[0].name],
          toName: d[getGeoCity[1].name],
          coords: [[fromCityInfo.lon, fromCityInfo.lat], [toCityInfo.lon, toCityInfo.lat]]
        }]
        scatterData = [{
          name: d[getGeoCity[1].name],
          value: [toCityInfo.lon, toCityInfo.lat, value]
        }]
      }
    } else if (getGeoProvince.length > 1 && d[getGeoProvince[0].name] && d[getGeoProvince[1].name]) {
      const fromProvinceInfo = getProvinceArea(d[getGeoProvince[0].name])
      const toProvinceInfo = getProvinceArea(d[getGeoProvince[1].name])

      if (fromProvinceInfo && toProvinceInfo) {
        legendData.push(d[getGeoProvince[0].name])
        linesSeriesData = [{
          fromName: d[getGeoProvince[0].name],
          toName: d[getGeoProvince[1].name],
          coords: [[fromProvinceInfo.lon, fromProvinceInfo.lat], [toProvinceInfo.lon, toProvinceInfo.lat]]
        }]
        scatterData = [{
          name: d[getGeoProvince[1].name],
          value: [toProvinceInfo.lon, toProvinceInfo.lat, value]
        }]
      }
    } else {
      return
    }

    effectScatterType = {
      name: '',
      type: 'effectScatter',
      coordinateSystem: 'geo',
      zlevel: index,
      rippleEffect: {
          brushType: 'stroke'
      },
      ...labelOptionLines,
      symbolSize: (val) => {
          return 12
      },

      data: scatterData
    }

    linesType = {
      name: '',
      type: 'lines',
      zlevel: index,
      symbol: ['none', 'arrow'],
      symbolSize: 10,
      effect: {
          show: true,
          // period: 600,
          trailLength: 0,
          symbol: symbolType,
          symbolSize: 15,
          constantSpeed: linesSpeed
      },
      lineStyle: {
          normal: {
              width: 2,
              opacity: 0.6,
              curveness: 0.2
          }
      },
      data: linesSeriesData
    }
    linesSeries.push(linesType, effectScatterType)
  })

  let legendOption
  if (chartStyles.legend) {
    legendOption = {
      legend: getLegendOption(chartStyles.legend, legendData)
    }
  } else {
    legendOption = null
  }
  const visualMapOptions: EChartOption.VisualMap = getVisualMapOptions(min, max, 'map', chartStyles)
  const { tooltip, geo, labelOption } = baseOption
  const {map, roam} = geo
  return {
    ...legendOption,
    geo: {
        map,
        roam
    },
    ...visualMapOptions,
    series: linesSeries
  }
}
function getProvinceParent(area) {
  if (!area.parent) {
    return area
  }
  const parent = geoData.find((g) => g.id === area.parent)
  return !parent.parent ? parent : getProvinceParent(parent)
}

function getProvinceName(name) {
  provinceSuffix.forEach((ps) => {
    if (name.includes(ps)) {
      name = name.replace(ps, '')
    }
  })
  return name
}

function getCityArea(name) {
  const hasSuffix = citySuffix.some((p) => name.includes(p))
  const area = hasSuffix
    ? geoData.find((d) => d.name === name)
    : geoData.find((d) => d.name.includes(name))
  return area
}

function getProvinceArea(name) {
  const hasSuffix = provinceSuffix.some((p) => name.includes(p))
  const area = hasSuffix
    ? geoData.find((d) => d.name === name && !d.parent)
    : geoData.find((d) => d.name.includes(name) && !d.parent)
  return area
}
