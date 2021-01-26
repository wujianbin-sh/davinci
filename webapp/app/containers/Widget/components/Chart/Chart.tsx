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
import React from 'react'
import { message as Message } from 'antd'
import { IChartProps } from './index'
import chartlibs from '../../config/chart'
import echarts from 'echarts/lib/echarts'
import { ECharts, EChartOption } from 'echarts'
import chartOptionGenerator from '../../render/chart'
import { isPromise } from 'utils/util'
const styles = require('./Chart.less')
const GeoLevels = ['city', 'province', 'country']
interface IChartStates {
  seriesItems: string[]
}
export class Chart extends React.PureComponent<IChartProps, IChartStates> {
  private asyncEmitTimer: NodeJS.Timer | null = null
  private container: HTMLDivElement = null
  private instance: ECharts
  private mapData = {}
  constructor(props) {
    super(props)
    this.state = {
      seriesItems: []
    }
  }
  public componentDidMount() {
    this.mapData = this.getMapData(this.props)
    this.renderChart(this.props, this.mapData)
  }

  public componentDidUpdate() {
    this.mapData = this.getMapData(this.props)
    this.renderChart(this.props, this.mapData)
  }
  private getMapData = (props: IChartProps) => {
    const mapData = { mapLevel: 'country', currentCode: 'china', drillEnable: false, limitLevel: 'province' }
    const { selectedChart, chartStyles } = props
    if (selectedChart !== 7) {
      return mapData
    }
    const { scope, drillLevel } = chartStyles
    if (scope && drillLevel) {
      mapData.limitLevel = drillLevel.level
      mapData.drillEnable = drillLevel.enabled
      // 由小到大
      for (let i = 0; i < GeoLevels.length; i++) {
        if (scope[GeoLevels[i]]) {
          mapData.mapLevel = GeoLevels[i]
          mapData.currentCode = scope[GeoLevels[i]]
          break
        }
      }
    }
    return mapData
  }
  private renderChart = (props: IChartProps, mapData) => {
    const {
      selectedChart,
      renderType,
      getDataDrillDetail,
      isDrilling,
      onError
    } = props
    if (renderType === 'loading') {
      return
    }
    if (!this.instance) {
      this.instance = echarts.init(this.container, 'default')
    } else {
      if (renderType === 'rerender') {
        this.instance.dispose()
        this.instance = echarts.init(this.container, 'default')
      }
      if (renderType === 'clear') {
        this.instance.clear()
      }
    }
    try {
      this.instance.off('click')
      this.instance.on('click', (params) => {
        // 如果是 地图 chartid =7
        if (selectedChart === 7) {
          mapData = this.getMapData(props)
          if (!params.data) {
            this.renderChart(props, mapData)
            return
          }
          const index = GeoLevels.indexOf(params.data.mapLevel)
          const limitIndex = GeoLevels.indexOf(mapData.limitLevel)
          if (!(mapData.drillEnable && index >= limitIndex)) {
            this.renderChart(props, mapData)
            return
          }
          const newMapData = { ...mapData }
          newMapData.mapLevel = params.data.mapLevel
          newMapData.currentCode = params.data.curMapCode
          newMapData.mapName = params.data.curMapName
          this.renderChart(props, newMapData)

        }
        this.collectSelectedItems(params)
      })
      const drillOptions = {
        instance: this.instance,
        isDrilling,
        mapData,
        getDataDrillDetail,
        selectedItems: this.props.selectedItems,
        callback: (seriesData) => {
          this.instance.off('click')
          this.instance.on('click', (params) => {
            this.collectSelectedItems(params, seriesData)
          })
        }
      }
      const option = chartOptionGenerator(
        chartlibs.find((cl) => cl.id === selectedChart).name,
        props,
        drillOptions
      )
      if (isPromise(option)) {
        (option as Promise<EChartOption>).then((newOption) => {
          this.instance.setOption(newOption)
          this.instance.resize()
        }).catch((err) => {
          console.log('EChartOption', err)

          Message.error('暂没有找到相关地图文件')
          return
        })
      } else {
        this.instance.setOption(
          (option as EChartOption)
        )
        this.instance.resize()
      }

    } catch (error) {
      if (onError) {
        onError(error)
      }
    }
  }

  public componentWillUnmount() {
    if (this.instance) {
      this.instance.off('click')
    }
    if (this.asyncEmitTimer) {
      clearTimeout(this.asyncEmitTimer)
    }
  }

  private collectSelectedItems = (params, seriesData?) => {
    const {
      data,
      selectedChart,
      onDoInteract,
      getDataDrillDetail,
      onSelectChartsItems,
      onCheckTableInteract
    } = this.props

    const { seriesItems } = this.state
    if (selectedChart === 7) {
      return
    }
    let selectedItems = []
    let series = []
    if (this.props.selectedItems && this.props.selectedItems.length) {
      selectedItems = [...this.props.selectedItems]
    }
    let dataIndex = params.dataIndex
    if (selectedChart === 4) {
      dataIndex = params.seriesIndex
    }
    if (selectedItems.length === 0) {
      selectedItems.push(dataIndex)
    } else {
      const isb = selectedItems.some((item) => item === dataIndex)
      if (isb) {
        for (let index = 0, l = selectedItems.length; index < l; index++) {
          if (selectedItems[index] === dataIndex) {
            selectedItems.splice(index, 1)
            break
          }
        }
      } else {
        selectedItems.push(dataIndex)
      }
    }

    if (seriesData) {
      const { seriesIndex, dataIndex } = params
      const char = `${seriesIndex}&${dataIndex}`
      if (seriesItems && Array.isArray(seriesItems)) {
        series = seriesItems.includes(char)
          ? seriesItems.filter((item) => item !== char)
          : seriesItems.concat(char)
        this.setState({ seriesItems: series })
      }
    }
    const resultData = selectedItems.map((item, index) => {
      if (seriesData) {
        const seriesIndex = series[index] ? series[index].split('&')[0] : null
        return seriesData[seriesIndex] ? seriesData[seriesIndex][item] : []
      }
      return data[item]
    })
    const brushed = [{ 0: Object.values(resultData) }]
    const sourceData = Object.values(resultData)
    const isInteractiveChart = onCheckTableInteract && onCheckTableInteract()
    if (isInteractiveChart && onDoInteract) {
      const triggerData = sourceData
      onDoInteract(triggerData)
    }
    this.asyncEmitTimer = setTimeout(() => {
      if (getDataDrillDetail) {
        getDataDrillDetail(JSON.stringify({ range: null, brushed, sourceData }))
      }
    }, 500)
    if (onSelectChartsItems) {
      onSelectChartsItems(selectedItems)
    }
  }

  public render() {
    return (
      <div
        className={styles.chartContainer}
        ref={(f) => (this.container = f)}
      />
    )
  }
}

export default Chart
