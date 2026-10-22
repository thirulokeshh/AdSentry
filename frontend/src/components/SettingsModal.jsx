import React, { useState, useEffect } from 'react';
import { api } from '../api';

export default function SettingsModal({ isOpen, onClose, onSettingsUpdated }) {
  const [settings, setSettings] = useState({
    globalCpaTarget: 500,
    minSpendThreshold: 1000,
    priorityHighSpendCutoff: 5000,
    priorityHighCpaMultiplier: 2.0
  });
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (isOpen) {
      loadSettings();
    }
  }, [isOpen]);

  const loadSettings = async () => {
    setLoading(true);
    setError(null);
    setSuccess(false);
    try {
      const data = await api.getSettings();
      setSettings(data);
    } catch (err) {
      setError(err.message || 'Failed to load settings.');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setSuccess(false);
    try {
      const updated = await api.updateSettings({
        globalCpaTarget: parseFloat(settings.globalCpaTarget),
        minSpendThreshold: parseFloat(settings.minSpendThreshold),
        priorityHighSpendCutoff: parseFloat(settings.priorityHighSpendCutoff),
        priorityHighCpaMultiplier: parseFloat(settings.priorityHighCpaMultiplier)
      });
      setSettings(updated);
      setSuccess(true);
      if (onSettingsUpdated) onSettingsUpdated();
      setTimeout(() => {
        onClose();
      }, 1000);
    } catch (err) {
      setError(err.message || 'Failed to update settings.');
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: '560px' }}>
        <div className="modal-header">
          <div className="modal-title">⚙️ Recommendation Threshold Settings</div>
          <button className="modal-close-btn" onClick={onClose}>&times;</button>
        </div>

        <div style={{ background: 'rgba(255, 255, 255, 0.04)', border: '1px solid var(--border-subtle)', borderRadius: '10px', padding: '12px', fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
          ℹ️ <strong>Note:</strong> All defaults are example thresholds for your evaluation, not universal industry benchmarks. Updating values immediately triggers background recalculation of all recommendations.
        </div>

        {error && (
          <div style={{ background: 'var(--accent-red-bg)', color: 'var(--accent-red)', padding: '10px 14px', borderRadius: '8px', fontSize: '0.85rem' }}>
            ⚠️ {error}
          </div>
        )}

        {success && (
          <div style={{ background: 'var(--accent-green-bg)', color: 'var(--accent-green)', padding: '10px 14px', borderRadius: '8px', fontSize: '0.85rem' }}>
            ✓ Settings successfully saved! Recalculating recommendations...
          </div>
        )}

        {loading ? (
          <div style={{ textAlign: 'center', padding: '24px', color: 'var(--text-muted)' }}>Loading settings...</div>
        ) : (
          <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Global Target CPA (₹ INR)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                className="form-input"
                value={settings.globalCpaTarget || ''}
                onChange={(e) => setSettings({ ...settings, globalCpaTarget: e.target.value })}
                required
              />
              <span className="form-help">Campaigns with CPA exceeding this amount may trigger Rule B (High CPA).</span>
            </div>

            <div className="form-group">
              <label className="form-label">Minimum Spend Threshold for Zero Conversions (₹ INR)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                className="form-input"
                value={settings.minSpendThreshold || ''}
                onChange={(e) => setSettings({ ...settings, minSpendThreshold: e.target.value })}
                required
              />
              <span className="form-help">Campaigns with spend above this threshold and 0 conversions trigger Rule A.</span>
            </div>

            <div className="form-group">
              <label className="form-label">High Priority Spend Cutoff (₹ INR)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                className="form-input"
                value={settings.priorityHighSpendCutoff || ''}
                onChange={(e) => setSettings({ ...settings, priorityHighSpendCutoff: e.target.value })}
              />
              <span className="form-help">Zero-conversion spend exceeding this cutoff is classified as HIGH priority.</span>
            </div>

            <div className="form-group">
              <label className="form-label">High Priority CPA Multiplier</label>
              <input
                type="number"
                step="0.1"
                min="1.0"
                className="form-input"
                value={settings.priorityHighCpaMultiplier || ''}
                onChange={(e) => setSettings({ ...settings, priorityHighCpaMultiplier: e.target.value })}
              />
              <span className="form-help">Multiple of target CPA required to classify Rule B as HIGH priority (e.g. 2.0x).</span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px' }}>
              <button type="button" className="btn" onClick={onClose} disabled={saving}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving...' : 'Save & Recalculate'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
