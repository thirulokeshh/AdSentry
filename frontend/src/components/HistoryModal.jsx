import React, { useState, useEffect } from 'react';
import { api } from '../api';

export default function HistoryModal({ isOpen, onClose, onDataChanged }) {
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedBatchErrors, setSelectedBatchErrors] = useState(null);
  const [errorLoading, setErrorLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isOpen) {
      loadBatches();
    }
  }, [isOpen]);

  const loadBatches = async () => {
    setLoading(true);
    setError(null);
    setSelectedBatchErrors(null);
    try {
      const data = await api.listBatches();
      setBatches(data || []);
    } catch (err) {
      setError(err.message || 'Failed to load import history.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (batchId) => {
    if (!window.confirm(`Are you sure you want to delete Batch #${batchId}? If this batch supplied winning records, the newest remaining version will be restored automatically.`)) {
      return;
    }
    try {
      await api.deleteBatch(batchId);
      await loadBatches();
      if (onDataChanged) onDataChanged();
    } catch (err) {
      setError(err.message || 'Failed to delete batch.');
    }
  };

  const handleViewErrors = async (batchId) => {
    setErrorLoading(true);
    try {
      const errorPage = await api.getBatchErrors(batchId, 0, 50);
      setSelectedBatchErrors({ batchId, errors: errorPage.content || [] });
    } catch (err) {
      setError(err.message || 'Failed to load row errors.');
    } finally {
      setErrorLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: '820px' }}>
        <div className="modal-header">
          <div className="modal-title">📜 Import History & Version Restorations</div>
          <button className="modal-close-btn" onClick={onClose}>&times;</button>
        </div>

        {error && (
          <div style={{ background: 'var(--accent-red-bg)', color: 'var(--accent-red)', padding: '10px 14px', borderRadius: '8px', fontSize: '0.85rem' }}>
            ⚠️ {error}
          </div>
        )}

        {loading ? (
          <div style={{ textAlign: 'center', padding: '32px', color: 'var(--text-muted)' }}>Loading history...</div>
        ) : batches.length === 0 ? (
          <div className="empty-state">
            <h3>No Import Batches Found</h3>
            <p>Upload a Google Ads or Meta Ads CSV to start auditing marketing campaigns.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="table-responsive">
              <table>
                <thead>
                  <tr>
                    <th>Batch</th>
                    <th>Platform</th>
                    <th>Filename</th>
                    <th>Accepted</th>
                    <th>Rejected</th>
                    <th>Duplicates</th>
                    <th>Imported At</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {batches.map((b) => (
                    <tr key={b.id}>
                      <td style={{ fontWeight: 700, color: '#fff' }}>#{b.id}</td>
                      <td>
                        <span className={`badge ${b.platform === 'Google Ads' ? 'badge-high' : 'badge-low'}`} style={{ color: '#fff' }}>
                          {b.platform}
                        </span>
                      </td>
                      <td style={{ maxWidth: '160px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={b.filename}>
                        {b.filename}
                      </td>
                      <td className="num" style={{ color: 'var(--accent-green)', fontWeight: 600 }}>{b.acceptedRows}</td>
                      <td className="num" style={{ color: b.rejectedRows > 0 ? 'var(--accent-red)' : 'var(--text-muted)' }}>
                        {b.rejectedRows > 0 ? (
                          <button
                            className="btn btn-sm"
                            style={{ background: 'var(--accent-red-bg)', color: 'var(--accent-red)', border: 'none', cursor: 'pointer' }}
                            onClick={() => handleViewErrors(b.id)}
                          >
                            {b.rejectedRows} Errors ↗
                          </button>
                        ) : (
                          '0'
                        )}
                      </td>
                      <td className="num" style={{ color: 'var(--accent-amber)' }}>{b.duplicateRows}</td>
                      <td style={{ fontSize: '0.78rem' }}>{b.importedAt ? new Date(b.importedAt).toLocaleDateString() : 'N/A'}</td>
                      <td>
                        <button
                          className="btn btn-sm"
                          style={{ color: 'var(--accent-red)', borderColor: 'rgba(239, 68, 68, 0.3)' }}
                          onClick={() => handleDelete(b.id)}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Row Errors Inspector */}
            {selectedBatchErrors && (
              <div style={{ background: 'var(--bg-surface-subtle)', border: '1px solid var(--border-subtle)', borderRadius: '12px', padding: '16px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                  <div style={{ fontWeight: 700, color: 'var(--accent-red)', fontSize: '0.9rem' }}>
                    Rejected Row Details for Batch #{selectedBatchErrors.batchId}
                  </div>
                  <button className="btn btn-sm" onClick={() => setSelectedBatchErrors(null)}>Hide</button>
                </div>
                {selectedBatchErrors.errors.length === 0 ? (
                  <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>No row errors recorded.</div>
                ) : (
                  <div style={{ maxHeight: '180px', overflowY: 'auto' }}>
                    {selectedBatchErrors.errors.map((err, idx) => (
                      <div key={idx} style={{ padding: '6px 0', borderBottom: '1px solid rgba(255, 255, 255, 0.04)', fontSize: '0.8rem', display: 'flex', gap: '12px' }}>
                        <span style={{ color: 'var(--text-muted)', width: '60px' }}>Row {err.sourceRowNumber}:</span>
                        <span style={{ color: 'var(--accent-red)', fontWeight: 600, width: '120px' }}>[{err.errorCode}]</span>
                        <span style={{ color: 'var(--text-secondary)' }}>{err.errorMessage}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
