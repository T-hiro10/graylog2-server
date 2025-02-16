// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';

import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';

type ChartDefinition = {
  type: string,
  name: string,
  x?: Array<string>,
  y?: Array<any>,
  opacity: number,
};

const getChartColor = (fullData, name) => {
  const data = fullData.find(d => (d.name === name)).marker;
  if (data && data.marker && data.marker.color) {
    const { marker: color } = data;
    return color;
  }
  return undefined;
};

const setChartColor = (chart, colors) => ({ marker: { color: colors[chart.name] } });

const BarVisualization: VisualizationComponent = ({ config, data, effectiveTimerange }: VisualizationComponentProps) => {
  const { visualizationConfig } = config;
  const layout = {};

  /* $FlowFixMe: type inheritance does not work here */
  if (visualizationConfig && visualizationConfig.barmode) {
    layout.barmode = visualizationConfig.barmode;
  }
  /* $FlowFixMe: type inheritance does not work here */
  const opacity = visualizationConfig ? visualizationConfig.opacity : 1.0;

  const _seriesGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values, opacity });

  return (
    <XYPlot config={config}
            chartData={chartData(config, data, 'bar', _seriesGenerator)}
            effectiveTimerange={effectiveTimerange}
            getChartColor={getChartColor}
            setChartColor={setChartColor}
            plotLayout={layout} />
  );
};

BarVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

BarVisualization.type = 'bar';

export default BarVisualization;
