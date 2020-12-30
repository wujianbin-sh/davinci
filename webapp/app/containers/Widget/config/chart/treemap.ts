import ChartTypes from './ChartTypes'
import {
  PIVOT_CHART_FONT_FAMILIES,
  PIVOT_DEFAULT_FONT_COLOR,
  CHART_LABEL_POSITIONS
} from 'app/globalConstants'
import { IChartInfo } from 'containers/Widget/components/Widget'
const defaultTheme = require('assets/json/echartsThemes/default.project.json')
const defaultThemeColors = defaultTheme.theme.color
const treeMap: IChartInfo = {
  id: ChartTypes.TreeMap,
  name: 'treemap',
  title: '矩形树图',
  icon: 'icon-chart-treemap',
  coordinate: 'cartesian',
  rules: [{ dimension: [1, 99], metric: [1, 2] }],
  dimetionAxis: 'col',
  data: {
    cols: {
      title: '列',
      type: 'category'
    },
    rows: {
      title: '行',
      type: 'category'
    },
    metrics: {
      title: '指标',
      type: 'value'
    },
    filters: {
      title: '筛选',
      type: 'all'
    }
  },
  style: {
    label: {
      showLabel: true,
      labelPosition: CHART_LABEL_POSITIONS[5].value,
      labelFontFamily: PIVOT_CHART_FONT_FAMILIES[0].value,
      labelFontSize: '12',
      labelColor: PIVOT_DEFAULT_FONT_COLOR,
      labelParts: ['dimensionValue', 'indicatorValue'],
      ellipsis: true
    },
    spec: {
      roam: false, //
      visualDimension: 1
    }
  }
}

export default treeMap
