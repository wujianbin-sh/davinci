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
import {
    getVisualMapOptions

} from '../utils'
export function getMapOption(
    chartProps: IChartProps,
    drillOptions,
    baseOption
) {
    const { chartStyles } = chartProps
    const { spec} = chartStyles
    const { roam } = spec
    const { mapData } = drillOptions
    const { dataTree, min, max, tooltip, labelOption, itemStyle } = baseOption
    const visualMapOptions: EChartOption.VisualMap = getVisualMapOptions(
        min,
        max,
        'map',
        chartStyles
    )
    const mapItem = visualMapOptions.show ? {} : itemStyle
    const seriesData = Object.keys(dataTree).map((key, index) => {
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
    localStorage.setItem('NewseriesData', JSON.stringify(seriesData))
    return {
        tooltip,
        ...visualMapOptions,
        series: {
            name: '地图',
            type: 'map',
            mapType: mapData.currentCode,
            roam,
            itemStyle: mapItem,
            ...labelOption,
            data: seriesData
        }
    }
}
