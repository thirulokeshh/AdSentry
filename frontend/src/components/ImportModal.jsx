import React, { useState } from 'react';
import { api } from '../api';

export default function ImportModal({ isOpen, onClose, onImportSuccess }) {
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [step, setStep] = useState(1); // 1: Select, 2: Preview & Map, 3: Completed Summary

  // Mapping & Confirmation State
  const [confirmedPlatform, setConfirmedPlatform] = useState('');
  const [conversionDefinition, setConversionDefinition] = useState('PURCHASE');
  const [importResult, setImportResult] = useState(null);

  if (!isOpen) return null;

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
      setError(null);
    }
  };

  const handlePreview = async () => {
    if (!file) {
      setError('Please select a CSV file first.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append('file', file);
      if (confirmedPlatform) formData.append('confirmedPlatform', confirmedPlatform);
      if (conversionDefinition) formData.append('conversionDefinition', conversionDefinition);

      const previewData = await api.previewImport(formData);
      setPreview(previewData);
      setConfirmedPlatform(previewData.detectedPlatform || 'Google Ads');
      setConversionDefinition(previewData.conversionDefinition || 'PURCHASE');
      setStep(2);
    } catch (err) {
      setError(err.message || 'Failed to preview CSV file.');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmImport = async () => {
    setLoading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append('file', file);
      if (confirmedPlatform) formData.append('confirmedPlatform', confirmedPlatform);
      formData.append('confirmedCurrency', 'INR');
      if (preview && preview.detectedDateFormat) {
        formData.append('dateFormat', preview.detectedDateFormat);
      }
      formData.append('conversionDefinition', conversionDefinition);

      const result = await api.confirmImport(formData);
      setImportResult(result);
      setStep(3);
      if (onImportSuccess) onImportSuccess();
    } catch (err) {
      setError(err.message || 'Failed to execute import.');
    } finally {
      setLoading(false);
    }
  };

  const resetModal = () => {
    setFile(null);
    setPreview(null);
    setError(null);
    setStep(1);
    setImportResult(null);
    onClose();
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: '780px' }}>
        <div className="modal-header">
          <div className="modal-title">
            📤 Import CSV Report {step === 2 && '— Preview & Mapping'} {step === 3 && '— Import Summary'}
          </div>
          <button className="modal-close-btn" onClick={resetModal}>&times;</button>
        </div>

        {error && (
          <div style={{ background: 'var(--accent-red-bg)', color: 'var(--accent-red)', padding: '10px 14px', borderRadius: '8px', fontSize: '0.85rem' }}>
            ⚠️ {error}
          </div>
        )}

        {/* STEP 1: FILE DROPZONE */}
        {step === 1 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div className="dropzone" onClick={() => document.getElementById('csvFileInput').click()}>
              <input
                id="csvFileInput"
                type="file"
                accept=".csv"
                style={{ display: 'none' }}
                onChange={handleFileChange}
              />
              <div style={{ fontSize: '2.4rem', marginBottom: '8px' }}>📄</div>
              <div style={{ fontWeight: 600, fontSize: '1.05rem', color: '#fff' }}>
                {file ? file.name : 'Click to select or drop a Google Ads or Meta Ads CSV'}
              </div>
              <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginTop: '6px' }}>
                Supported formats: Native Google Ads / Meta Ads Manager exports (INR currency required)
              </div>
            </div>

            {file && (
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button className="btn btn-primary" onClick={handlePreview} disabled={loading}>
                  {loading ? 'Inspecting CSV...' : 'Inspect & Preview Mappings →'}
                </button>
              </div>
            )}
          </div>
        )}

        {/* STEP 2: PREVIEW & CONFIRMATION */}
        {step === 2 && preview && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Detection Summary Cards */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px' }}>
              <div className="form-group" style={{ background: 'var(--bg-surface-subtle)', padding: '12px', borderRadius: '8px' }}>
                <span className="form-help">Detected Platform</span>
                <span style={{ fontWeight: 700, color: '#fff' }}>{preview.detectedPlatform}</span>
                <span style={{ fontSize: '0.72rem', color: 'var(--accent-green)' }}>{preview.platformConfidence} Confidence</span>
              </div>
              <div className="form-group" style={{ background: 'var(--bg-surface-subtle)', padding: '12px', borderRadius: '8px' }}>
                <span className="form-help">Currency Check</span>
                <span style={{ fontWeight: 700, color: preview.detectedCurrency === 'INR' ? 'var(--accent-green)' : 'var(--accent-red)' }}>
                  {preview.detectedCurrency || 'INR'}
                </span>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>INR Only Enforced</span>
              </div>
              <div className="form-group" style={{ background: 'var(--bg-surface-subtle)', padding: '12px', borderRadius: '8px' }}>
                <span className="form-help">Observed Dates</span>
                <span style={{ fontWeight: 700, fontSize: '0.85rem', color: '#fff' }}>
                  {preview.observedStartDate || 'Unknown'} to {preview.observedEndDate || 'Unknown'}
                </span>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Format: {preview.detectedDateFormat || 'Auto'}</span>
              </div>
              <div className="form-group" style={{ background: 'var(--bg-surface-subtle)', padding: '12px', borderRadius: '8px' }}>
                <span className="form-help">Row Breakdown</span>
                <span style={{ fontWeight: 700, color: '#fff' }}>
                  {preview.estimatedValidRows} Valid / {preview.estimatedInvalidRows} Invalid
                </span>
                <span style={{ fontSize: '0.72rem', color: 'var(--accent-amber)' }}>{preview.detectedDuplicatesCount} Overlaps</span>
              </div>
            </div>

            {/* Platform & Conversion Overrides */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label">Platform Confirmation</label>
                <select
                  className="form-input"
                  value={confirmedPlatform}
                  onChange={(e) => setConfirmedPlatform(e.target.value)}
                >
                  <option value="Google Ads">Google Ads</option>
                  <option value="Meta Ads">Meta Ads</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Conversion Definition</label>
                <select
                  className="form-input"
                  value={conversionDefinition}
                  onChange={(e) => setConversionDefinition(e.target.value)}
                >
                  <option value="PURCHASE">Purchase / Orders</option>
                  <option value="LEAD">Leads / Registrations</option>
                  <option value="MESSAGING">Messaging Conversations</option>
                  <option value="GENERIC_CONVERSION">Generic Conversions</option>
                </select>
              </div>
            </div>

            {/* Mappings */}
            <div>
              <div style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '8px' }}>
                Recognized Column Mappings
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                {Object.entries(preview.columnMappings || {}).map(([col, field]) => (
                  <span key={col} style={{ background: 'rgba(255, 255, 255, 0.06)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.76rem' }}>
                    <strong>{col}</strong> → <span style={{ color: 'var(--accent-blue)' }}>{field}</span>
                  </span>
                ))}
              </div>
            </div>

            {/* Action Buttons */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
              <button className="btn" onClick={() => setStep(1)} disabled={loading}>
                ← Back
              </button>
              <button className="btn btn-primary" onClick={handleConfirmImport} disabled={loading}>
                {loading ? 'Processing Import...' : `Confirm & Import ${preview.estimatedValidRows} Records →`}
              </button>
            </div>
          </div>
        )}

        {/* STEP 3: COMPLETED SUMMARY */}
        {step === 3 && importResult && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            <div style={{ background: 'var(--accent-green-bg)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: '12px', padding: '16px', color: '#fff' }}>
              <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--accent-green)', marginBottom: '4px' }}>
                ✓ Import Successfully Completed
              </div>
              <div style={{ fontSize: '0.88rem' }}>
                Batch ID #{importResult.id} for {importResult.platform} ({importResult.filename}) has been committed.
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px' }}>
              <div className="comparison-stat-box">
                <span className="form-help">Accepted Rows</span>
                <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--accent-green)' }} className="num">
                  {importResult.acceptedRows}
                </div>
              </div>
              <div className="comparison-stat-box">
                <span className="form-help">Rejected Rows</span>
                <div style={{ fontSize: '1.4rem', fontWeight: 800, color: importResult.rejectedRows > 0 ? 'var(--accent-red)' : 'var(--text-muted)' }} className="num">
                  {importResult.rejectedRows}
                </div>
              </div>
              <div className="comparison-stat-box">
                <span className="form-help">Duplicates Resolved</span>
                <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--accent-amber)' }} className="num">
                  {importResult.duplicateRows}
                </div>
              </div>
              <div className="comparison-stat-box">
                <span className="form-help">Reporting Period</span>
                <div style={{ fontSize: '0.85rem', fontWeight: 700, marginTop: '6px', color: '#fff' }}>
                  {importResult.reportingStartDate || 'N/A'} to {importResult.reportingEndDate || 'N/A'}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '12px' }}>
              <button className="btn btn-primary" onClick={resetModal}>
                Close & View Updated Dashboard
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
