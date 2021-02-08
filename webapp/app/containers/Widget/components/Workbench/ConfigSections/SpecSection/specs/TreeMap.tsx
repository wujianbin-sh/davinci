import React from 'react'
import { Row, Col, Checkbox, Select } from 'antd'
const Option = Select.Option
import { onSectionChange } from './util'
import { ISpecConfig } from '../types'
import { chartLayerTypeOptions, chartSymbolTypeOptions } from '../../constants'

import styles from '../../../Workbench.less'

interface ISpecSectionTreeMapProps {
    spec: ISpecConfig
    title: string
    onChange: (value: string | number, propPath: string | string[]) => void
}

function SpecSectionTreeMap(props: ISpecSectionTreeMapProps) {
    const { spec, title, onChange } = props
    const { roam, visualDimension } = spec

    return (
        <div className={styles.paneBlock}>
            <h4>{title}</h4>
            <div className={styles.blockBody}>
                <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
                    <Col span={10}>
                        <Checkbox checked={roam} onChange={onSectionChange(onChange, 'roam')}>
                            移动&缩放
                        </Checkbox>
                    </Col>

                </Row>
                <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
                    <Col span={10}>
                        颜色映射指标
                    </Col>
                    <Col span={10}>

                        <Select
                            placeholder="指标索引"
                            className={styles.blockElm}
                            value={visualDimension}
                            onChange={onSectionChange(onChange, 'visualDimension')}
                        >
                            {[0, 1].map((item) => (<Option key={item} value={item}>{item}</Option>))}
                        </Select>
                    </Col>
                </Row>
            </div>
        </div>
    )
}

export default SpecSectionTreeMap
