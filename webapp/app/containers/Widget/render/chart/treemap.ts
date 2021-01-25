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

import { IChartProps } from '../../components/Chart'
import { decodeMetricName } from '../../components/util'
import { getLabelOption } from './util'
import echarts, { EChartOption } from 'echarts'
import { getFormattedValue } from '../../components/Config/Format'
import defaultTheme from 'assets/json/echartsThemes/default.project.json'
const defaultThemeColors = defaultTheme.theme.color
const treeMapValueKey = 'treeMapValue'

export default function (chartProps: IChartProps) {
  const { data, cols, metrics, chartStyles } = chartProps

  const keyList = cols.map((col) => col.name)
  const valueKeyList = metrics.map((metric) => {
    return `${metric.agg}(${decodeMetricName(metric.name)})`
  })

  const hasValueKeyList = data.map((it) => {
    const item = { ...it }
    item[treeMapValueKey] = []
    valueKeyList.forEach((key) => {
      item[treeMapValueKey].push(it[key])
    })
    return item
  })

  const seriesData = listToTree(hasValueKeyList, keyList, 0, treeMapValueKey)
  console.log('seriesData', seriesData)

  const { label, toolbox, spec } = chartStyles
  const labelOption = {
    label: getLabelOption('treemap', label, metrics)
  }
  const valueSimpleKeyList = metrics.map((metric) => {
    return decodeMetricName(metric.name)
  })
  const { roam, visualDimension } = spec
  const tooltip = {
    trigger: 'item',
    formatter(params) {
      const { treePathInfo, value } = params
      const treePath = []
      for (let i = 1; i < treePathInfo.length; i++) {
        treePath.push(treePathInfo[i].name)
      }
      const tooltipLabels = []
      const basicInfo = treePath.filter((item) => !!item).join('/')
      tooltipLabels.push(basicInfo)
      if (Array.isArray(value)) {
        for (let i = 0; i < value.length; i++) {
          if (valueSimpleKeyList[i]) {
            tooltipLabels.push(`${valueSimpleKeyList[i]}: ${value[i]}`)
          }
        }
      } else {
        tooltipLabels.push(value)
      }
      return tooltipLabels.join('<br/>')
    }
  }
  return {
    tooltip: { ...tooltip },
    series: [
      {
        type: 'treemap',
        ...labelOption,
        roam,
        visualDimension: visualDimension,
        breadcrumb: {
          show: false
        },
        upperLabel: {
          show: false
        },
        levels: getLevelOption(),
        data: seriesData
      }
    ]
  }
  function listToTree(array, keyList, index, treeMapValueKey) {
    const groups = {}
    array.forEach((item) => {
      const group = item[keyList[index]]
      groups[group] = groups[group] || []
      groups[group].push(item)
    })
    const keyArr = Object.keys(groups)
    const arr = keyArr.map((key) => {
      if (index === keyList.length - 1) {
        return {
          name: key,
          value: groups[key][0][treeMapValueKey]
        }
      } else {
        return {
          name: key,
          children: listToTree(groups[key], keyList, index + 1, treeMapValueKey)
        }
      }
    })
    if (keyList.length === 1) {
      return [
        {
          name: '',
          children: arr
        }
      ]
    }
    return arr
  }
  function getLevelOption() {
    const levels = []
    const { cols, chartStyles } = chartProps
    const { label } = chartStyles
    const { labelParts } = label
    // label.showLabel
    const showUpperLabel = label.showLabel && labelParts.includes('upperLabel')
    const upperLabel = {
      normal: {
        show: showUpperLabel,
        position: label.labelPosition,
        color: label.labelColor,
        fontFamily: label.labelFontFamily,
        fontSize: label.labelFontSize
      }
    }

    if (cols.length === 1) {
      levels.push({
        itemStyle: {
          borderWidth: 4,
          gapWidth: 4
        },
        upperLabel: {
          normal: {
            show: false
          }
        }
      })
      levels.push({
        colorSaturation: [0.6, 0.3],
        itemStyle: {
          borderColorSaturation: 0.7,
          gapWidth: 2,
          borderWidth: 2
        },
        upperLabel: {
          normal: {
            show: false
          }
        }
      })
    }
    if (cols.length > 1) {
      levels.push({
        colorSaturation: [0.6, 0.3],
        itemStyle: {
          borderColorSaturation: 0.7,
          borderWidth: 4,
          gapWidth: 4
        },
        upperLabel: { ...upperLabel }
      })
      levels.push({
        colorSaturation: [0.6, 0.3],
        itemStyle: {
          borderColorSaturation: 0.7,
          gapWidth: 2,
          borderWidth: 2
        },
        upperLabel: { ...upperLabel }
      })
      levels.push({
        colorSaturation: [0.6, 0.3],
        itemStyle: {
          borderColorSaturation: 0.7,
          gapWidth: 2,
          borderWidth: 2
        },
        upperLabel: { ...upperLabel }
      })
    }
    return levels
  }
}
