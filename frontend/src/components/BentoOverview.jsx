import React from 'react';

export default function BentoOverview({
  overview,
  campaigns = [],
  comparison,
  coverage,
  onViewWasteClick
}) {
  const metrics = overview?.metrics || {};

  // Compute total at-risk / zero-conversion spend
  const zeroConvCampaigns = campaigns.filter(
    (c) => c.metrics && (c.metrics.conversions === 0 || c.metrics.conversions === '0.00' || c.metrics.conversions === null) && parseFloat(c.metrics.spend || 0) > 0
  );

  const atRiskSpend = zeroConvCampaigns.reduce(
    (acc, curr) => acc + parseFloat(curr.metrics?.spend || 0),
    0
  );

  // Compute platform spend share
  const googleSpend = campaigns
    .filter((c) => c.platform === 'Google Ads')
    .reduce((acc, curr) => acc + parseFloat(curr.metrics?.spend || 0), 0);

  const metaSpend = campaigns
    .filter((c) => c.platform === 'Meta Ads')
    .reduce((acc, curr) => acc + parseFloat(curr.metrics?.spend || 0), 0);

  const totalPlatformSpend = googleSpend + metaSpend;
  const googlePercent = totalPlatformSpend > 0 ? Math.round((googleSpend / totalPlatformSpend) * 100) : 0;
  const metaPercent = totalPlatformSpend > 0 ? Math.round((metaSpend / totalPlatformSpend) * 100) : 0;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Top Bento Row */}
      <div className="bento-grid">
        {/* Hero At-Risk Waste Card */}
        <div className="bento-card card-hero-alert">
          <div>
            <div className="card-label">🚨 Total At-Risk Spend</div>
            <div className="card-value num">
              ₹{atRiskSpend.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </div>
            <div className="card-subtext">
              {zeroConvCampaigns.length} Campaign{zeroConvCampaigns.length === 1 ? '' : 's'} recorded zero known conversions
            </div>
          </div>

          <div className="hero-action-row">
            <span style={{ fontSize: '0.8rem', opacity: 0.9 }}>
              Rule A: Spend &gt; Threshold with 0 Conv.
            </span>
            <button
              className="btn-circle-action"
              onClick={onViewWasteClick}
              title="Jump to Recommendations Feed"
            >
              ↗
            </button>
          </div>
        </div>

        {/* Total Performance Card */}
        <div className="bento-card card-performance">
          <div>
            <div className="card-label">
              <span>Overall Ad Spend</span>
              <span style={{ color: 'var(--text-muted)', fontWeight: 500, fontSize: '0.74rem' }}>
                Currency: INR (₹)
              </span>
            </div>
            <div className="card-value num">
              ₹{metrics.spend ? parseFloat(metrics.spend).toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00'}
            </div>
          </div>

          <div className="metrics-horizontal-strip">
            <div className="metric-sub-item">
              <div className="sub-label">Blended CPA</div>
              <div className="sub-value num" style={{ color: metrics.cpa ? 'var(--text-primary)' : 'var(--text-muted)' }}>
                {metrics.cpa ? `₹${parseFloat(metrics.cpa).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '—'}
              </div>
            </div>

            <div className="metric-sub-item">
              <div className="sub-label">Clicks</div>
              <div className="sub-value num">
                {metrics.clicks ? metrics.clicks.toLocaleString() : '0'}
              </div>
            </div>

            <div className="metric-sub-item">
              <div className="sub-label">Avg CTR</div>
              <div className="sub-value num" style={{ color: 'var(--accent-green)' }}>
                {metrics.ctr ? `${metrics.ctr}%` : '—'}
              </div>
            </div>

            <div className="metric-sub-item">
              <div className="sub-label">Avg CPC</div>
              <div className="sub-value num">
                {metrics.cpc ? `₹${metrics.cpc}` : '—'}
              </div>
            </div>
          </div>
        </div>

        {/* Platform Share Bento Card with Striped Texture */}
        <div className="bento-card card-platform-share">
          <div>
            <div className="card-label">Platform Share</div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginTop: '4px' }}>
              Normalized spend split
            </div>
          </div>

          <div className="platform-bar-row">
            <div>
              <div className="platform-bar-header">
                <span style={{ color: '#10b981' }}>Google Ads ({googlePercent}%)</span>
                <span className="num">₹{googleSpend.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</span>
              </div>
              <div className="bar-container" style={{ marginTop: '4px' }}>
                <div className="bar-striped-bg"></div>
                <div className="bar-fill fill-google" style={{ width: `${googlePercent}%` }}></div>
              </div>
            </div>

            <div>
              <div className="platform-bar-header">
                <span style={{ color: '#3b82f6' }}>Meta Ads ({metaPercent}%)</span>
                <span className="num">₹{metaSpend.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</span>
              </div>
              <div className="bar-container" style={{ marginTop: '4px' }}>
                <div className="bar-striped-bg"></div>
                <div className="bar-fill fill-meta" style={{ width: `${metaPercent}%` }}></div>
              </div>
            </div>
          </div>

          <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', marginTop: '12px' }}>
            {overview?.totalCampaigns || 0} active campaign{overview?.totalCampaigns === 1 ? '' : 's'} across {overview?.totalRecords || 0} effective daily records.
          </div>
        </div>
      </div>

      {/* Second Bento Row: Comparison & Coverage */}
      <div className="bento-grid">
        {/* Adjacent Period Comparison Card */}
        <div className="bento-card card-comparison">
          <div className="card-label">
            <span>Adjacent Equal-Length Period Comparison</span>
            {comparison && (
              <span className={`badge ${comparison.comparisonReliable ? 'badge-high' : 'badge-medium'}`} style={{ color: '#fff' }}>
                {comparison.comparisonReliable ? '✓ Reliable Comparison' : '⚠️ Coverage Gap Warning'}
              </span>
            )}
          </div>

          {comparison ? (
            <div className="comparison-grid">
              <div className="comparison-stat-box">
                <div className="form-help">Current vs Previous Spend</div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '4px' }}>
                  <span className="num" style={{ fontWeight: 800, fontSize: '1.2rem', color: '#fff' }}>
                    ₹{comparison.currentMetrics?.spend ? parseFloat(comparison.currentMetrics.spend).toLocaleString('en-IN') : '0'}
                  </span>
                  {comparison.deltas?.spend && (
                    <span className={`delta-badge ${comparison.deltas.spend.percentageDelta > 0 ? 'delta-negative' : 'delta-positive'}`}>
                      {comparison.deltas.spend.percentageDelta > 0 ? '+' : ''}{comparison.deltas.spend.percentageDelta}%
                    </span>
                  )}
                </div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Prev: ₹{comparison.previousMetrics?.spend ? parseFloat(comparison.previousMetrics.spend).toLocaleString('en-IN') : '0'}
                </div>
              </div>

              <div className="comparison-stat-box">
                <div className="form-help">Clicks Delta</div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '4px' }}>
                  <span className="num" style={{ fontWeight: 800, fontSize: '1.2rem', color: '#fff' }}>
                    {comparison.currentMetrics?.clicks ? comparison.currentMetrics.clicks.toLocaleString() : '0'}
                  </span>
                  {comparison.deltas?.clicks && (
                    <span className={`delta-badge ${comparison.deltas.clicks.percentageDelta >= 0 ? 'delta-positive' : 'delta-negative'}`}>
                      {comparison.deltas.clicks.percentageDelta > 0 ? '+' : ''}{comparison.deltas.clicks.percentageDelta}%
                    </span>
                  )}
                </div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Prev: {comparison.previousMetrics?.clicks ? comparison.previousMetrics.clicks.toLocaleString() : '0'}
                </div>
              </div>

              <div className="comparison-stat-box">
                <div className="form-help">Blended CPA Shift</div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '4px' }}>
                  <span className="num" style={{ fontWeight: 800, fontSize: '1.2rem', color: '#fff' }}>
                    {comparison.currentMetrics?.cpa ? `₹${parseFloat(comparison.currentMetrics.cpa).toLocaleString('en-IN')}` : '—'}
                  </span>
                  {comparison.deltas?.cpa && comparison.deltas.cpa.percentageDelta !== null && (
                    <span className={`delta-badge ${comparison.deltas.cpa.percentageDelta <= 0 ? 'delta-positive' : 'delta-negative'}`}>
                      {comparison.deltas.cpa.percentageDelta > 0 ? '+' : ''}{comparison.deltas.cpa.percentageDelta}%
                    </span>
                  )}
                </div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Prev: {comparison.previousMetrics?.cpa ? `₹${parseFloat(comparison.previousMetrics.cpa).toLocaleString('en-IN')}` : '—'}
                </div>
              </div>
            </div>
          ) : (
            <div style={{ color: 'var(--text-muted)', padding: '20px 0', fontSize: '0.85rem' }}>
              Import historical periods to calculate adjacent equal-length comparisons.
            </div>
          )}
        </div>

        {/* Data Coverage Health Card */}
        <div className="bento-card card-coverage">
          <div className="card-label">
            <span>Data Coverage Health</span>
            {coverage && (
              <span style={{ color: coverage.sufficientCoverage ? 'var(--accent-green)' : 'var(--accent-amber)', fontWeight: 700 }}>
                {coverage.coveragePercentage}% Complete
              </span>
            )}
          </div>

          {coverage ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '12px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px' }}>
                <div className="comparison-stat-box">
                  <div className="form-help">Calendar Days</div>
                  <div className="num" style={{ fontSize: '1.2rem', fontWeight: 700, color: '#fff' }}>
                    {coverage.totalCalendarDays}d
                  </div>
                </div>
                <div className="comparison-stat-box">
                  <div className="form-help">Active Days</div>
                  <div className="num" style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--accent-green)' }}>
                    {coverage.activeDays}d
                  </div>
                </div>
                <div className="comparison-stat-box">
                  <div className="form-help">Missing Days</div>
                  <div className="num" style={{ fontSize: '1.2rem', fontWeight: 700, color: coverage.missingDays > 0 ? 'var(--accent-amber)' : 'var(--text-muted)' }}>
                    {coverage.missingDays}d
                  </div>
                </div>
              </div>

              {coverage.warnings && coverage.warnings.length > 0 && (
                <div className="warning-chip">
                  ⚠️ {coverage.warnings[0]}
                </div>
              )}
            </div>
          ) : (
            <div style={{ color: 'var(--text-muted)', padding: '20px 0', fontSize: '0.85rem' }}>
              No coverage information available.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
