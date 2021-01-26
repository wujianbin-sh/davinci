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
import { getSizeRate} from '../../../../components/util'
import {
  getVisualMapOptions
} from '../utils'
import {getSymbolSize} from '../../util'
export function getHeatmapOption(chartProps: IChartProps, drillOptions, baseOption) {

const { chartStyles } = chartProps
const { spec} = chartStyles
const { roam } = spec
const {mapData } = drillOptions
const { dataTree, min, max, geo, tooltip, labelOption, itemStyle } = baseOption
const sizeRate = getSizeRate(min, max)
const visualMapOptions: EChartOption.VisualMap = getVisualMapOptions(min, max, 'map', chartStyles)

const mapSeriesData = Object.keys(dataTree).map((key, index) => {
        const { lon, lat, value, mapLevel, curMapCode, curMapName  } = dataTree[key]
        return {
            name: key,
            value: [lon, lat, value],
            mapLevel,
            curMapCode,
            curMapName,
            children: []
        }
    })
return {
    geo: {
        ...geo,
        label: {
            emphasis: {
                show: true
            }
          }
        },
    ...visualMapOptions,
    series: [
      {
       name: '热力图',
       type: 'heatmap',
       coordinateSystem: 'geo',
       roam,
       data: Object.keys(dataTree).map((key, index) => {
          const { lon, lat, value, mapLevel, curMapCode, curMapName } = dataTree[key]
          return {
            name: key,
            value: [lon, lat, value],
            symbolSize: getSymbolSize(sizeRate, value),
            curMapCode,
            mapLevel,
            curMapName
          }
        }),
       ...labelOption

    },
     {
        type: 'map',
        map: mapData.currentCode,
        geoIndex: 0,
        roam,
        label: {
          normal: {
            show: false
          },
          emphasis: {
            show: false
          }
        },
        itemStyle,
        labelOption,
        data: mapSeriesData
      }
    ]
  }
}
