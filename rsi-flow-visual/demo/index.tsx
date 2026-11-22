import React, { useEffect, useRef, useState } from 'react';
import ReactDOM from 'react-dom';
import { ViewState, VisualToolbarConfig } from 'types';
import VisualDiagram from '../src';
import { randomString } from './test/TEST_SIMPLE_VIEW/utils/RandomUtils';
import {
  canvasConfig,
  viewStateTemplate,
} from './test/TEST_COMPETITION_RESEARCH/ViewStateTemplate';

const API_BASE = 'http://localhost:8088';
const FLOW_ID = 'FLOW_COMPETITION_RESEARCH';

const SKILL_OPTIONS = ['算法', '系统底层', '硬件嵌入式', '建模写作', '项目产品'];

type RunResult = {
  flowId: string;
  grade?: string;
  motive?: string;
  skills?: string;
  hours?: string;
  goalType?: string;
  rulePrimary?: string;
  ruleBackup?: string;
  primaryCompetitions?: string;
  backupCompetitions?: string;
  explanation?: string;
  catalogWindow?: string;
  llmUsed?: boolean;
  adjusted?: boolean;
  error?: string;
};

const fieldStyle: React.CSSProperties = {
  width: '100%',
  marginTop: 4,
  marginBottom: 10,
  padding: '8px 10px',
  borderRadius: 8,
  border: '1px solid #d1d5db',
  boxSizing: 'border-box',
};

const App = () => {
  const visualDiagramRef = useRef<any>(null);
  const [grade, setGrade] = useState('大三');
  const [motive, setMotive] = useState('冲推免');
  const [skills, setSkills] = useState<string[]>(['算法']);
  const [hours, setHours] = useState('10-20');
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(false);
  const [apiOk, setApiOk] = useState<boolean | null>(null);
  const [result, setResult] = useState<RunResult | null>(null);
  const [error, setError] = useState('');

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
      visualDiagramRef.current.deleteElement('buildProfile');
    }
  };

  const toolbarConfig: VisualToolbarConfig = [
    { id: 'create', onClick: onCreateModel, icon: 'plus', tips: '新增模型' },
    { id: 'delete', onClick: onDeleteModel, icon: 'delete', tips: '删除模型' },
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

  useEffect(() => {
    fetch(`${API_BASE}/api/health`)
      .then((r) => r.json())
      .then(() => setApiOk(true))
      .catch(() => setApiOk(false));
  }, []);

  const toggleSkill = (sk: string) => {
    setSkills((prev) =>
      prev.indexOf(sk) >= 0 ? prev.filter((x) => x !== sk) : prev.concat([sk]),
    );
  };

  const handleChange = (viewState: ViewState) => {
    console.log('onChange', viewState);
  };

  const runFlow = () => {
    setLoading(true);
    setError('');
    setResult(null);
    fetch(`${API_BASE}/api/competition/run`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ grade, motive, skills, hours, note }),
    })
      .then((resp) =>
        resp.json().then((data: RunResult) => {
          if (!resp.ok) {
            throw new Error(data.error || `HTTP ${resp.status}`);
          }
          return data;
        }),
      )
      .then((data) => {
        setResult(data);
        setApiOk(true);
      })
      .catch((e) => {
        setApiOk(false);
        setError(
          (e && e.message) ||
            '请求失败。请先启动后端 API（scripts/start-competition-api.ps1）',
        );
      })
      .then(() => setLoading(false));
  };

  return (
    <div style={{ width: '100%', height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <div
        style={{
          padding: '10px 16px',
          fontFamily: 'Segoe UI, PingFang SC, Microsoft YaHei, sans-serif',
          borderBottom: '1px solid #e5e7eb',
          background: '#fafafa',
          display: 'flex',
          justifyContent: 'space-between',
          gap: 12,
          alignItems: 'center',
          flexWrap: 'wrap',
        }}
      >
        <div>
          <strong>任务1 · 竞赛调研流程</strong>
          <span style={{ marginLeft: 10, color: '#666', fontSize: 13 }}>
            flowId = {FLOW_ID}
          </span>
        </div>
        <div style={{ fontSize: 12, color: apiOk ? '#389e0d' : '#cf1322' }}>
          API {apiOk == null ? '检测中…' : apiOk ? '已连接 :8088' : '未连接（请先起 CompetitionApiServer）'}
        </div>
      </div>

      <div style={{ flex: 1, display: 'flex', minHeight: 0 }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <VisualDiagram
            canvasConfig={visualCanvasConfig}
            ref={visualDiagramRef}
            onChange={handleChange}
          />
        </div>

        <aside
          style={{
            width: 360,
            borderLeft: '1px solid #e5e7eb',
            background: '#fff',
            padding: 16,
            overflow: 'auto',
            fontFamily: 'Segoe UI, PingFang SC, Microsoft YaHei, sans-serif',
            boxSizing: 'border-box',
          }}
        >
          <div style={{ fontWeight: 600, marginBottom: 8 }}>选手画像</div>
          <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 12 }}>
            结构化表单 → 规则保底 → LLM 可调整名单
          </div>

          <label style={{ fontSize: 12, color: '#374151' }}>年级</label>
          <select value={grade} onChange={(e) => setGrade(e.target.value)} style={fieldStyle}>
            <option value="大一">大一</option>
            <option value="大二">大二</option>
            <option value="大三">大三</option>
            <option value="大四">大四</option>
          </select>

          <label style={{ fontSize: 12, color: '#374151' }}>动机</label>
          <select value={motive} onChange={(e) => setMotive(e.target.value)} style={fieldStyle}>
            <option value="冲推免">冲推免</option>
            <option value="练能力">练能力</option>
            <option value="补简历">补简历</option>
          </select>

          <label style={{ fontSize: 12, color: '#374151' }}>技能（可多选）</label>
          <div style={{ marginTop: 6, marginBottom: 12 }}>
            {SKILL_OPTIONS.map((sk) => (
              <label
                key={sk}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  marginRight: 10,
                  marginBottom: 6,
                  fontSize: 12,
                  color: '#374151',
                }}
              >
                <input
                  type="checkbox"
                  checked={skills.indexOf(sk) >= 0}
                  onChange={() => toggleSkill(sk)}
                  style={{ marginRight: 4 }}
                />
                {sk}
              </label>
            ))}
          </div>

          <label style={{ fontSize: 12, color: '#374151' }}>每周投入</label>
          <select value={hours} onChange={(e) => setHours(e.target.value)} style={fieldStyle}>
            <option value="5以下">5以下</option>
            <option value="5-10">5-10</option>
            <option value="10-20">10-20</option>
            <option value="20以上">20以上</option>
          </select>

          <label style={{ fontSize: 12, color: '#374151' }}>备注（可选）</label>
          <textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="例如：已有校队训练，想兼顾 ASC"
            rows={2}
            style={{ ...fieldStyle, resize: 'vertical' }}
          />

          <button
            type="button"
            onClick={runFlow}
            disabled={loading}
            style={{
              width: '100%',
              padding: '10px 12px',
              border: 'none',
              borderRadius: 8,
              background: loading ? '#93c5fd' : '#2563eb',
              color: '#fff',
              fontWeight: 600,
              cursor: loading ? 'wait' : 'pointer',
            }}
          >
            {loading ? '执行中…' : '运行 FLOW_COMPETITION_RESEARCH'}
          </button>

          {error ? (
            <div
              style={{
                marginTop: 12,
                padding: 10,
                background: '#fff1f0',
                color: '#a8071a',
                borderRadius: 8,
                fontSize: 12,
                whiteSpace: 'pre-wrap',
              }}
            >
              {error}
            </div>
          ) : null}

          {result ? (
            <div style={{ marginTop: 16, fontSize: 13, lineHeight: 1.55 }}>
              <div style={{ fontWeight: 600, marginBottom: 8 }}>执行结果</div>
              <Row k="goalType" v={String(result.goalType || '')} />
              <Row k="final primary" v={String(result.primaryCompetitions || '')} />
              <Row k="final backup" v={String(result.backupCompetitions || '')} />
              <Row k="llmUsed" v={String(!!result.llmUsed)} />
              <Row k="adjusted" v={String(!!result.adjusted)} />
              {result.adjusted ? (
                <div
                  style={{
                    marginTop: 8,
                    padding: 8,
                    background: '#f0f5ff',
                    borderRadius: 8,
                    fontSize: 12,
                  }}
                >
                  <div style={{ fontWeight: 600, marginBottom: 4 }}>规则保底（对比）</div>
                  <div>primary：{result.rulePrimary}</div>
                  <div>backup：{result.ruleBackup}</div>
                </div>
              ) : (
                <Row k="rulePrimary" v={String(result.rulePrimary || '')} />
              )}
              <div style={{ marginTop: 10, color: '#4b5563', whiteSpace: 'pre-wrap' }}>
                {result.explanation}
              </div>
            </div>
          ) : null}

          <div style={{ marginTop: 20, fontSize: 11, color: '#9ca3af' }}>
            数据：competition/catalog.json
            <br />
            规则保底 + LLM 在白名单内可调整
          </div>
        </aside>
      </div>
    </div>
  );
};

const Row = ({ k, v }: { k: string; v: string }) => (
  <div style={{ marginBottom: 6 }}>
    <span style={{ color: '#6b7280' }}>{k}：</span>
    <span style={{ color: '#111827' }}>{v}</span>
  </div>
);

ReactDOM.render(<App></App>, document.getElementById('root'));
