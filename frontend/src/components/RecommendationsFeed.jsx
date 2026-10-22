import React, { useState } from 'react';
import { api } from '../api';

export default function RecommendationsFeed({
  recommendations = [],
  onStatusUpdated,
  isDemo = false
}) {
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [actionLoading, setActionLoading] = useState(null);
  const [runningAnalysis, setRunningAnalysis] = useState(false);
  const [analysisSummary, setAnalysisSummary] = useState(null);

  const handleStatusChange = async (id, newStatus) => {
    if (isDemo) {
      alert('Status mutations are disabled in isolated Demo Mode.');
      return;
    }
    setActionLoading(id);
    try {
      await api.updateRecommendationStatus(id, newStatus);
      if (onStatusUpdated) onStatusUpdated();
    } catch (err) {
      alert(err.message || 'Failed to update status.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleRunAnalysis = async () => {
    if (isDemo) {
      alert('Analysis recalculation is disabled in isolated Demo Mode.');
      return;
    }
    setRunningAnalysis(true);
    setAnalysisSummary(null);
    try {
      const res = await api.runAnalysis({});
      setAnalysisSummary(`Analyzed ${res.totalAnalyzedCampaigns} campaigns: ${res.newRecommendationsCount} new, ${res.retainedRecommendationsCount} retained, ${res.retiredRecommendationsCount} retired.`);
      if (onStatusUpdated) onStatusUpdated();
    } catch (err) {
      alert(err.message || 'Failed to trigger analysis run.');
    } finally {
      setRunningAnalysis(false);
    }
  };

  const filtered = recommendations.filter((r) => {
    if (statusFilter !== 'ALL' && r.status !== statusFilter) return false;
    if (priorityFilter !== 'ALL' && r.priority !== priorityFilter) return false;
    return true;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      <div className="recommendations-header">
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff', display: 'flex', alignItems: 'center', gap: '8px' }}>
            🎯 Actionable Recommendations
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 500 }}>
              ({filtered.length} of {recommendations.length} Active Findings)
            </span>
          </h2>
          <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
            Transparent rule-based findings. Recommendations will never make automatic campaign edits.
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          {/* Priority filter */}
          <div className="segmented-control">
            <button
              className={`segmented-btn ${priorityFilter === 'ALL' ? 'active' : ''}`}
              onClick={() => setPriorityFilter('ALL')}
            >
              All Priorities
            </button>
            <button
              className={`segmented-btn ${priorityFilter === 'HIGH' ? 'active' : ''}`}
              onClick={() => setPriorityFilter('HIGH')}
            >
              High
            </button>
            <button
              className={`segmented-btn ${priorityFilter === 'MEDIUM' ? 'active' : ''}`}
              onClick={() => setPriorityFilter('MEDIUM')}
            >
              Medium
            </button>
          </div>

          {/* Status filter */}
          <div className="segmented-control">
            <button
              className={`segmented-btn ${statusFilter === 'ALL' ? 'active' : ''}`}
              onClick={() => setStatusFilter('ALL')}
            >
              All
            </button>
            <button
              className={`segmented-btn ${statusFilter === 'NEW' ? 'active' : ''}`}
              onClick={() => setStatusFilter('NEW')}
            >
              New
            </button>
            <button
              className={`segmented-btn ${statusFilter === 'REVIEWED' ? 'active' : ''}`}
              onClick={() => setStatusFilter('REVIEWED')}
            >
              Reviewed
            </button>
            <button
              className={`segmented-btn ${statusFilter === 'DISMISSED' ? 'active' : ''}`}
              onClick={() => setStatusFilter('DISMISSED')}
            >
              Dismissed
            </button>
          </div>

          <button
            className="btn"
            onClick={handleRunAnalysis}
            disabled={runningAnalysis || isDemo}
            title="Recalculates all recommendation rules against the latest effective data"
          >
            {runningAnalysis ? 'Analyzing...' : '🔄 Re-Run Rules'}
          </button>
        </div>
      </div>

      {analysisSummary && (
        <div style={{ background: 'var(--accent-blue-bg)', color: '#60a5fa', padding: '10px 14px', borderRadius: '8px', fontSize: '0.84rem' }}>
          ℹ️ {analysisSummary}
        </div>
      )}

      {filtered.length === 0 ? (
        <div className="bento-card" style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--text-muted)' }}>
          <div style={{ fontSize: '2rem', marginBottom: '8px' }}>✓</div>
          <div style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>No recommendations match the selected filters.</div>
          <div style={{ fontSize: '0.82rem', marginTop: '4px' }}>All campaigns are currently performing within configured thresholds.</div>
        </div>
      ) : (
        <div className="recommendations-list">
          {filtered.map((r) => {
            const pClass = r.priority === 'HIGH' ? 'priority-high' : r.priority === 'MEDIUM' ? 'priority-medium' : 'priority-low';
            const sClass = r.status ? r.status.toLowerCase() : 'new';

            return (
              <div key={r.id || r.fingerprint} className={`rec-card ${pClass}`}>
                <div className="rec-top-row">
                  <div className="rec-badge-group">
                    <span className={`badge ${r.priority === 'HIGH' ? 'badge-high' : r.priority === 'MEDIUM' ? 'badge-medium' : 'badge-low'}`}>
                      {r.priority} Priority
                    </span>
                    <span className={`badge badge-status ${sClass}`}>
                      Status: {r.status}
                    </span>
                    <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)', fontWeight: 600 }}>
                      {r.platform} • {r.campaignName}
                    </span>
                  </div>

                  <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                    Period: {r.periodStart || 'N/A'} to {r.periodEnd || 'N/A'}
                  </span>
                </div>

                <div className="rec-title">{r.title}</div>
                <div className="rec-explanation">{r.explanation}</div>

                {r.suggestedAction && (
                  <div className="rec-action-box">
                    <strong>Suggested Action:</strong> {r.suggestedAction}
                  </div>
                )}

                <div className="rec-footer">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    <span>Trigger: <code style={{ color: 'var(--text-secondary)' }}>{r.triggerCondition || 'Exceeded threshold'}</code></span>
                  </div>

                  <div className="rec-btn-group">
                    {r.status === 'NEW' && (
                      <>
                        <button
                          className="btn btn-sm"
                          onClick={() => handleStatusChange(r.id, 'REVIEWED')}
                          disabled={actionLoading === r.id}
                        >
                          Mark Reviewed
                        </button>
                        <button
                          className="btn btn-sm"
                          style={{ color: 'var(--text-muted)' }}
                          onClick={() => handleStatusChange(r.id, 'DISMISSED')}
                          disabled={actionLoading === r.id}
                        >
                          Dismiss
                        </button>
                      </>
                    )}

                    {r.status === 'REVIEWED' && (
                      <>
                        <button
                          className="btn btn-sm"
                          style={{ color: 'var(--text-muted)' }}
                          onClick={() => handleStatusChange(r.id, 'DISMISSED')}
                          disabled={actionLoading === r.id}
                        >
                          Dismiss
                        </button>
                        <button
                          className="btn btn-sm"
                          onClick={() => handleStatusChange(r.id, 'NEW')}
                          disabled={actionLoading === r.id}
                        >
                          Reset to New
                        </button>
                      </>
                    )}

                    {r.status === 'DISMISSED' && (
                      <button
                        className="btn btn-sm"
                        onClick={() => handleStatusChange(r.id, 'NEW')}
                        disabled={actionLoading === r.id}
                      >
                        Restore to New
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
