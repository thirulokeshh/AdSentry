import React, { useState, useEffect, useRef } from 'react';
import { api } from './api';
import BentoOverview from './components/BentoOverview';
import RecommendationsFeed from './components/RecommendationsFeed';
import CampaignAuditTable from './components/CampaignAuditTable';
import ImportModal from './components/ImportModal';
import SettingsModal from './components/SettingsModal';
import HistoryModal from './components/HistoryModal';

export default function App() {
  // Global Filters
  const [platform, setPlatform] = useState('ALL');
  const [dateRangeMode, setDateRangeMode] = useState('ALL'); // 'ALL', '30D', '7D'
  const [isDemo, setIsDemo] = useState(false);

  // Data State
  const [overview, setOverview] = useState(null);
  const [campaigns, setCampaigns] = useState([]);
  const [comparison, setComparison] = useState(null);
  const [coverage, setCoverage] = useState(null);
  const [recommendations, setRecommendations] = useState([]);

  // UI Modals State
  const [isImportOpen, setIsImportOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [exportMenuOpen, setExportMenuOpen] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const recSectionRef = useRef(null);

  // Calculate start & end date based on dateRangeMode
  const getDateRange = () => {
    if (dateRangeMode === 'ALL') {
      return { startDate: undefined, endDate: undefined };
    }
    const end = new Date();
    const start = new Date();
    if (dateRangeMode === '30D') {
      start.setDate(end.getDate() - 30);
    } else if (dateRangeMode === '7D') {
      start.setDate(end.getDate() - 7);
    }
    const fmt = (d) => d.toISOString().split('T')[0];
    return { startDate: fmt(start), endDate: fmt(end) };
  };

  const loadAllData = async () => {
    setLoading(true);
    setError(null);
    try {
      const { startDate, endDate } = getDateRange();
      const params = { startDate, endDate, platform, isDemo };

      const [overviewData, campaignsData, comparisonData, coverageData, recsData] = await Promise.all([
        api.getOverview(params).catch(() => null),
        api.getCampaigns(params).catch(() => ({ campaigns: [] })),
        !isDemo ? api.getComparison(params).catch(() => null) : null,
        !isDemo ? api.getCoverage(params).catch(() => null) : null,
        api.getRecommendations(params).catch(() => [])
      ]);

      setOverview(overviewData);
      setCampaigns(campaignsData?.campaigns || []);
      setComparison(comparisonData);
      setCoverage(coverageData);
      setRecommendations(recsData || []);
    } catch (err) {
      setError(err.message || 'Failed to load marketing analytics data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAllData();
  }, [platform, dateRangeMode, isDemo]);

  const handleScrollToRecommendations = () => {
    if (recSectionRef.current) {
      recSectionRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const { startDate, endDate } = getDateRange();

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* 1. Shared Workspace Security Banner */}
      <div className="workspace-banner">
        <div>
          ⚠️ <strong>Shared Workspace Notice:</strong> Initial MVP instance with local evaluation storage. Non-private. Do not upload live customer credentials or sensitive PII.
        </div>
        <div style={{ display: 'flex', gap: '16px' }}>
          <span>Currencies Supported: <strong>INR (₹) Only</strong></span>
          <span style={{ opacity: 0.8 }}>No AI APIs • No Auto-Campaign Changes</span>
        </div>
      </div>

      {/* 2. Top Header Navigation */}
      <header className="app-header">
        <div className="brand">
          <div className="brand-logo">
            <span>🛡️</span>
            <span>AdSentry</span>
          </div>
          <span className="brand-badge">Audit MVP</span>
        </div>

        <div className="header-controls">
          {/* Platform Segmented Control */}
          <div className="segmented-control">
            <button
              className={`segmented-btn ${platform === 'ALL' ? 'active' : ''}`}
              onClick={() => setPlatform('ALL')}
            >
              All Platforms
            </button>
            <button
              className={`segmented-btn ${platform === 'Google Ads' ? 'active' : ''}`}
              onClick={() => setPlatform('Google Ads')}
            >
              Google Ads
            </button>
            <button
              className={`segmented-btn ${platform === 'Meta Ads' ? 'active' : ''}`}
              onClick={() => setPlatform('Meta Ads')}
            >
              Meta Ads
            </button>
          </div>

          {/* Date Segmented Control */}
          <div className="segmented-control">
            <button
              className={`segmented-btn ${dateRangeMode === 'ALL' ? 'active' : ''}`}
              onClick={() => setDateRangeMode('ALL')}
            >
              All Dates
            </button>
            <button
              className={`segmented-btn ${dateRangeMode === '30D' ? 'active' : ''}`}
              onClick={() => setDateRangeMode('30D')}
            >
              Last 30 Days
            </button>
            <button
              className={`segmented-btn ${dateRangeMode === '7D' ? 'active' : ''}`}
              onClick={() => setDateRangeMode('7D')}
            >
              Last 7 Days
            </button>
          </div>

          {/* Isolated Demo Mode Toggle */}
          <button
            className={`demo-toggle-btn ${isDemo ? 'active' : ''}`}
            onClick={() => setIsDemo(!isDemo)}
            title="Toggle isolated demo dataset (never mutates real database records)"
          >
            <span>{isDemo ? '● Demo Active' : '○ Demo Mode'}</span>
          </button>

          {/* Export Dropdown */}
          <div style={{ position: 'relative' }}>
            <button
              className="btn btn-sm"
              onClick={() => setExportMenuOpen(!exportMenuOpen)}
            >
              📥 Export ▾
            </button>
            {exportMenuOpen && (
              <div
                style={{
                  position: 'absolute',
                  top: '100%',
                  right: 0,
                  marginTop: '6px',
                  background: 'var(--bg-surface-elevated)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: '8px',
                  boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
                  zIndex: 200,
                  minWidth: '200px',
                  display: 'flex',
                  flexDirection: 'column'
                }}
              >
                <a
                  href={isDemo ? '#' : api.getCampaignsExportUrl({ startDate, endDate, platform })}
                  onClick={(e) => {
                    setExportMenuOpen(false);
                    if (isDemo) {
                      e.preventDefault();
                      alert('Exports are disabled in Demo Mode.');
                    }
                  }}
                  download="campaigns_export.csv"
                  style={{ padding: '10px 14px', color: '#fff', textDecoration: 'none', fontSize: '0.84rem', borderBottom: '1px solid rgba(255,255,255,0.06)' }}
                >
                  📊 Export Campaigns CSV
                </a>
                <a
                  href={isDemo ? '#' : api.getRecommendationsExportUrl({ startDate, endDate, platform })}
                  onClick={(e) => {
                    setExportMenuOpen(false);
                    if (isDemo) {
                      e.preventDefault();
                      alert('Exports are disabled in Demo Mode.');
                    }
                  }}
                  download="recommendations_export.csv"
                  style={{ padding: '10px 14px', color: '#fff', textDecoration: 'none', fontSize: '0.84rem' }}
                >
                  🎯 Export Recommendations CSV
                </a>
              </div>
            )}
          </div>

          <button className="btn btn-sm" onClick={() => setIsHistoryOpen(true)}>
            📜 History
          </button>

          <button className="btn btn-sm" onClick={() => setIsSettingsOpen(true)}>
            ⚙️ Settings
          </button>

          <button className="btn btn-primary btn-sm" onClick={() => setIsImportOpen(true)}>
            📤 Import CSV
          </button>
        </div>
      </header>

      {/* 3. Main Body */}
      <main className="dashboard-container" style={{ flex: 1 }}>
        {error && (
          <div style={{ background: 'var(--accent-red-bg)', color: 'var(--accent-red)', padding: '14px 18px', borderRadius: '12px', fontSize: '0.9rem' }}>
            ⚠️ <strong>Error loading data:</strong> {error}
          </div>
        )}

        {isDemo && (
          <div style={{ background: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.3)', color: 'var(--accent-amber)', padding: '10px 16px', borderRadius: '12px', fontSize: '0.88rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>
              ℹ️ <strong>Isolated Demo Mode Active:</strong> Displaying simulated reference performance across Google Ads and Meta Ads. Database tables remain untouched.
            </span>
            <button className="btn btn-sm" onClick={() => setIsDemo(false)} style={{ background: 'var(--accent-amber)', color: '#000', border: 'none' }}>
              Exit Demo Mode
            </button>
          </div>
        )}

        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)' }}>
            <div style={{ fontSize: '2rem', marginBottom: '10px' }}>⏳</div>
            <div>Loading AdSentry Marketing Audit...</div>
          </div>
        ) : campaigns.length === 0 && !isDemo ? (
          <div className="bento-card" style={{ textAlign: 'center', padding: '60px 20px' }}>
            <div style={{ fontSize: '3rem', marginBottom: '16px' }}>📊</div>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#fff', marginBottom: '8px' }}>
              No Marketing Data Imported Yet
            </h2>
            <p style={{ color: 'var(--text-muted)', maxWidth: '520px', margin: '0 auto 20px auto', fontSize: '0.92rem' }}>
              Import your first Google Ads or Meta Ads CSV report, or turn on <strong>Demo Mode</strong> above to preview AdSentry with realistic sample data.
            </p>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '12px' }}>
              <button className="btn btn-hero" onClick={() => setIsImportOpen(true)}>
                📤 Import CSV Report
              </button>
              <button className="btn" onClick={() => setIsDemo(true)}>
                Preview with Demo Data
              </button>
            </div>
          </div>
        ) : (
          <>
            {/* Bento Overview Cards */}
            <BentoOverview
              overview={overview}
              campaigns={campaigns}
              comparison={comparison}
              coverage={coverage}
              onViewWasteClick={handleScrollToRecommendations}
            />

            {/* Recommendations Section */}
            <div ref={recSectionRef}>
              <RecommendationsFeed
                recommendations={recommendations}
                onStatusUpdated={loadAllData}
                isDemo={isDemo}
              />
            </div>

            {/* Campaign Performance Audit Table */}
            <CampaignAuditTable
              campaigns={campaigns}
              currency={overview?.currency || 'INR'}
              startDate={startDate}
              endDate={endDate}
              platform={platform}
              isDemo={isDemo}
            />
          </>
        )}
      </main>

      {/* Footer */}
      <footer style={{ padding: '20px 28px', borderTop: '1px solid var(--border-subtle)', background: 'var(--bg-surface)', fontSize: '0.78rem', color: 'var(--text-muted)', display: 'flex', justifyContent: 'space-between' }}>
        <div>AdSentry v0.1-mvp • Small Business Advertising Audit Monolith</div>
        <div>Strictly Backend Metric Math • ISO Currency: INR (₹)</div>
      </footer>

      {/* Modals */}
      <ImportModal
        isOpen={isImportOpen}
        onClose={() => setIsImportOpen(false)}
        onImportSuccess={() => {
          setIsDemo(false);
          loadAllData();
        }}
      />

      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        onSettingsUpdated={loadAllData}
      />

      <HistoryModal
        isOpen={isHistoryOpen}
        onClose={() => setIsHistoryOpen(false)}
        onDataChanged={loadAllData}
      />
    </div>
  );
}
