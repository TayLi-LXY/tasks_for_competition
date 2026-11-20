import React, { useEffect, useRef } from 'react';
import ReactDOM from 'react-dom';
import { ViewState, VisualToolbarConfig } from 'types';
import VisualDiagram from '../src';
import { randomString } from './test/TEST_SIMPLE_VIEW/utils/RandomUtils';
import {
  canvasConfig,
  viewStateTemplate,
} from './test/TEST_COMPETITION_RESEARCH/ViewStateTemplate';

const App = () => {
  const visualDiagramRef = useRef<any>(null);

  const visualCanvasConfig = { ...canvasConfig };

  let initX = 100;

  const onCreateModel = () => {
    const type = 'model';
    const id = randomString(32);
    const elementState = {
      id,
      name: `新增${id}`,
      type,
      color: '#4099ff',
      x: (initX += 100),
      y: 100,
      select: false,
    };
    if (visualDiagramRef.current) {
      visualDiagramRef.current.createElement(elementState);
    }
  };

  const onDeleteModel = () => {
    if (visualDiagramRef.current) {
      visualDiagramRef.current.deleteElement('defineGoal');
    }
  };

  const toolbarConfig: VisualToolbarConfig = [
    {
      id: 'create',
      onClick: onCreateModel,
      icon: 'plus',
      tips: '新增模型',
    },
    {
      id: 'delete',
      onClick: onDeleteModel,
      icon: 'delete',
      tips: '删除模型',
    },
  ];

  visualCanvasConfig.toolbarConfig = toolbarConfig;

  useEffect(() => {
    if (visualDiagramRef.current) {
      visualDiagramRef.current.startLoading();
      visualDiagramRef.current.setView(viewStateTemplate);
      visualDiagramRef.current.selectToolbarItems([
        'create',
        'delete',
        'zoomIn',
        'zoomOut',
        'undo',
        'redo',
        'sort',
        'cut',
      ]);
      setTimeout(() => visualDiagramRef.current.endLoading(), 800);
    }
  }, []);

  const handleChange = (viewState: ViewState) => {
    console.log('onChange', viewState);
  };

  return (
    <div style={{ width: '100%', height: '100vh' }}>
      <div
        style={{
          padding: '12px 20px',
          fontFamily: 'sans-serif',
          borderBottom: '1px solid #eee',
          background: '#fafafa',
        }}
      >
        <strong>任务1 · 竞赛调研流程</strong>
        <span style={{ marginLeft: 12, color: '#666' }}>
          一年内适合参加的比赛选型（基于认定表 + 推免计分办法）
        </span>
      </div>
      <div style={{ width: '100%', height: 'calc(100vh - 52px)' }}>
        <VisualDiagram
          canvasConfig={visualCanvasConfig}
          ref={visualDiagramRef}
          onChange={handleChange}
        />
      </div>
    </div>
  );
};

ReactDOM.render(<App></App>, document.getElementById('root'));
