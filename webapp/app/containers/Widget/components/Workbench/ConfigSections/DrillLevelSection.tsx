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
import { Row, Col, Select, Checkbox } from 'antd'
import { mapDrillLevelOptions } from './constants'
const styles = require('../Workbench.less')
import { IDrillLeavel } from './'

interface IDrillLeavelProps {
    title: string
    config: IDrillLeavel
    onChange: (prop: string, value: any) => void
}
export class DrillLevelSection extends React.PureComponent<
    IDrillLeavelProps,
    {}
    > {
    private selectChange = (prop) => (value) => {
        this.props.onChange(prop, value)
    }
    private checkboxChange = (prop) => (e) => {
        this.props.onChange(prop, e.target.checked)
    }

    public render() {
        const { title, config } = this.props
        const { enabled, level } = config

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
                        <Col span={12}>
                            <Checkbox
                                checked={enabled}
                                onChange={this.checkboxChange('enabled')}
                            >
                                开启下钻
                            </Checkbox>
                        </Col>
                        <Col span={12}>
                            <Select
                                placeholder="下钻级别"
                                className={styles.blockElm}
                                value={level}
                                onChange={this.selectChange('level')}
                            >
                                {mapDrillLevelOptions}
                            </Select>
                        </Col>
                    </Row>
                </div>
            </div>
        )
    }
}

export default DrillLevelSection
