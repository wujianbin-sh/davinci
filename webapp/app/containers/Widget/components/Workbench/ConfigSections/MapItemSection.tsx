import React from 'react'
import { Row, Col, Select } from 'antd'
const Option = Select.Option
import ColorPicker from 'components/ColorPicker'
import { PIVOT_CHART_LINE_STYLES } from 'app/globalConstants'
const styles = require('../Workbench.less')
import { IMapItemConfig } from './'

interface IMapItemSectionProps {
    title: string
    config: IMapItemConfig
    onChange: (prop: string, value: any) => void
}

export class MapItemSection extends React.PureComponent<
    IMapItemSectionProps,
    {}
    > {

    private selectChange = (prop) => (value) => {
        this.props.onChange(prop, value)
    }

    private colorChange = (prop) => (color) => {
        this.props.onChange(prop, color)
    }

    public render() {
        const { title, config } = this.props

        const {
            areaColor,
            areaColorEmphasis,
            borderColor,
            borderWidth,
            borderType
        } = config
        const borderStyles = PIVOT_CHART_LINE_STYLES.map((l) => (
            <Option key={l.value} value={l.value}>
                {l.name}
            </Option>
        ))
        return (
            <div className={styles.paneBlock}>
                <h4>{title}</h4>
                <div className={styles.blockBody}>
                    <Row
                        gutter={8}
                        type="flex"
                        align="middle"
                        className={styles.blockRow}
                    >
                        <Col span={8}>初始颜色</Col>
                        <Col span={4}>
                            <ColorPicker
                                value={areaColor}
                                onChange={this.colorChange('areaColor')}
                            />
                        </Col>
                        <Col span={8}>高亮颜色</Col>
                        <Col span={4}>
                            <ColorPicker
                                value={areaColorEmphasis}
                                onChange={this.colorChange('areaColorEmphasis')}
                            />
                        </Col>
                    </Row>
                    <Row
                        gutter={8}
                        type="flex"
                        align="middle"
                        className={styles.blockRow}
                    >
                        <Col span={24}>地图区域描边样式</Col>
                    </Row>
                    <Row
                        gutter={8}
                        type="flex"
                        align="middle"
                        className={styles.blockRow}
                    >
                        <Col span={10}>
                            <Select
                                placeholder="样式"
                                className={styles.blockElm}
                                value={borderType}
                                onChange={this.selectChange('borderType')}
                            >
                                {borderStyles}
                            </Select>
                        </Col>
                        <Col span={10}>
                            <Select
                                placeholder="粗细"
                                className={styles.blockElm}
                                value={borderWidth}
                                onChange={this.selectChange('borderWidth')}
                            >
                                {Array.from(Array(10), (o, i) => (
                                    <Option key={i} value={`${i + 1}`}>
                                        {i + 1}
                                    </Option>
                                ))}
                            </Select>
                        </Col>
                        <Col span={4}>
                            <ColorPicker
                                value={borderColor}
                                onChange={this.colorChange('borderColor')}
                            />
                        </Col>
                    </Row>
                </div>
            </div>
        )
    }
}

export default MapItemSection
