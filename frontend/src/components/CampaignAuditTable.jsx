import React, { useState } from 'react';
import { api } from '../api';

export default function CampaignAuditTable({
  campaigns = [],
  currency = 'INR',
  startDate,
  endDate,
  platform,
  isDemo = false
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState('spend');
  const [sortDirection, setSortDirection] = useState('desc');

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const getMetricValue = (c, field) => {
    if (!c.metrics) return 0;
    const val = c.metrics[field];
    return val !== null && val !== undefined ? parseFloat(val) : -1;
  };

  const filtered = campaigns
    .filter((c) => {
      if (!searchTerm) return true;
      return (
        c.campaignName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.platform.toLowerCase().includes(searchTerm.toLowerCase())
      );
    })
    .sort((a, b) => {
      let valA, valB;
      if (sortField === 'campaignName' || sortField === 'platform') {
        valA = a[sortField].toLowerCase();
        valB = b[sortField].toLowerCase();
        return sortDirection === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA);
      } else {
        valA = getMetricValue(a, sortField);
        valB = getMetricValue(b, sortField);
        return sortDirection === 'asc' ? valA - valB : valB - valA;
      }
    });

  const exportUrl = api.getCampaignsExportUrl({ startDate, endDate, platform });

  return (
    <div className="table-card">
      <div className="table-header-row">
        <div>
          <h2 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#fff' }}>
            📊 Campaign Performance Audit
          </h2>
          <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
            Calculated strictly from aggregated totals. Undefined CPA (0 conversions) is left blank.
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <input
            type="text"
            className="form-input"
            style={{ width: '220px', padding: '6px 12px', fontSize: '0.84rem' }}
            placeholder="Search campaigns..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />

          <a
            href={isDemo ? '#' : exportUrl}
            onClick={(e) => {
              if (isDemo) {
                e.preventDefault();
                alert('CSV export from real database is disabled during isolated Demo Mode.');
              }
            }}
            className="btn btn-sm"
            download="campaigns_export.csv"
            style={{ textDecoration: 'none' }}
          >
            📥 Export CSV
          </a>
        </div>
      </div>

      <div className="table-responsive">
        <table>
          <thead>
            <tr>
              <th onClick={() => handleSort('campaignName')} style={{ cursor: 'pointer' }}>
                Campaign Name {sortField === 'campaignName' && (sortDirection === 'asc' ? '▲' : '▼')}
              </th>
              <th onClick={() => handleSort('platform')} style={{ cursor: 'pointer' }}>
                Platform {sortField === 'platform' && (sortDirection === 'asc' ? '▲' : '▼')}
              </th>
              <th>Active Days</th>
              <th onClick={() => handleSort('spend')} style={{ cursor: 'pointer' }}>
                Spend ({currency}) {sortField === 'spend' && (sortDirection === 'asc' ? '▲' : '▼')}
              </th>
              <th onClick={() => handleSort('clicks')} style={{ cursor: 'pointer' }}>
                Clicks {sortField === 'clicks' && (sortDirection === 'asc' ? '▲' : '▼')}
              </th>
              <th onClick={() => handleSort('ctr')} style={{ cursor: 'pointer' }}>
                CTR (%) {sortField === 'ctr' && (sortDirection === 'asc' ? '▲' : '▼')}
              </th>
              <th onClick={() => handleSort('cpc')} style={{ cursor: 'pointer' }}>
                CPC ({currency}) {sortField === 'cpc' && (sortDirection === 'asc' ? '▲' : '▼')}
              </th>
              <th onClick={() => handleSort('conversions')} style={{ cursor: 'pointer' }}>
                Conversions {sortField === 'conversions' && (sortDirection === 'asc' ? '▲' : '▼')}
              </th>
              <th onClick={() => handleSort('cpa')} style={{ cursor: 'pointer' }}>
                CPA ({currency}) {sortField === 'cpa' && (sortDirection === 'asc' ? '▲' : '▼')}
              </th>
              <th onClick={() => handleSort('conversionRate')} style={{ cursor: 'pointer' }}>
                Conv. Rate {sortField === 'conversionRate' && (sortDirection === 'asc' ? '▲' : '▼')}
              </th>
              <th>Confidence</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan="11" style={{ textAlign: 'center', padding: '36px', color: 'var(--text-muted)' }}>
                  No campaigns found matching "{searchTerm}".
                </td>
              </tr>
            ) : (
              filtered.map((c) => {
                const m = c.metrics || {};
                const hasCpa = m.cpa !== null && m.cpa !== undefined;

                return (
                  <tr key={c.campaignIdentityKey}>
                    <td>
                      <div style={{ fontWeight: 600, color: '#fff' }}>{c.campaignName}</div>
                      {c.warnings && c.warnings.length > 0 && (
                        <div className="warning-chip">
                          ⚠️ {c.warnings[0]}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${c.platform === 'Google Ads' ? 'badge-high' : 'badge-low'}`} style={{ color: '#fff' }}>
                        {c.platform}
                      </span>
                    </td>
                    <td className="num">{c.activeDays}d</td>
                    <td className="num" style={{ fontWeight: 700, color: '#fff' }}>
                      ₹{m.spend ? parseFloat(m.spend).toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00'}
                    </td>
                    <td className="num">{m.clicks ? m.clicks.toLocaleString() : '0'}</td>
                    <td className="num">{m.ctr ? `${m.ctr}%` : '—'}</td>
                    <td className="num">{m.cpc ? `₹${m.cpc}` : '—'}</td>
                    <td className="num" style={{ fontWeight: 600 }}>
                      {m.conversions !== null && m.conversions !== undefined ? m.conversions : '—'}
                    </td>
                    <td className="num" style={{ fontWeight: 700, color: hasCpa ? 'var(--text-primary)' : 'var(--text-muted)' }}>
                      {hasCpa ? `₹${parseFloat(m.cpa).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '—'}
                    </td>
                    <td className="num">{m.conversionRate ? `${m.conversionRate}%` : '—'}</td>
                    <td>
                      <span style={{ fontSize: '0.72rem', color: c.identityConfidence === 'HIGH_ID' ? 'var(--accent-green)' : 'var(--accent-amber)' }}>
                        {c.identityConfidence === 'HIGH_ID' ? 'High (ID)' : 'Medium (Name)'}
                      </span>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
