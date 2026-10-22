/**
 * AdSentry API Client
 * Connects directly to Spring Boot backend endpoints.
 * Handles both Live Data and isolated Demo Mode data.
 */
const RAW_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
const BASE_URL = RAW_BASE_URL.replace(/\/+$/, '');

async function handleResponse(response) {
  if (!response.ok) {
    let errorDetail = `Request failed with status ${response.status}`;
    try {
      const errorJson = await response.json();
      errorDetail = errorJson.detail || errorJson.message || errorDetail;
    } catch {
      // ignore JSON parse failure
    }
    throw new Error(errorDetail);
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

export const api = {
  // Analytics
  getOverview: async ({ startDate, endDate, platform, isDemo = false }) => {
    if (isDemo) {
      return handleResponse(await fetch(`${BASE_URL}/api/demo/overview`));
    }
    const params = new URLSearchParams();
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    if (platform && platform !== 'ALL') params.set('platform', platform);
    return handleResponse(await fetch(`${BASE_URL}/api/analytics/overview?${params.toString()}`));
  },

  getCampaigns: async ({ startDate, endDate, platform, isDemo = false }) => {
    if (isDemo) {
      return handleResponse(await fetch(`${BASE_URL}/api/demo/campaigns`));
    }
    const params = new URLSearchParams();
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    if (platform && platform !== 'ALL') params.set('platform', platform);
    return handleResponse(await fetch(`${BASE_URL}/api/analytics/campaigns?${params.toString()}`));
  },

  getComparison: async ({ startDate, endDate, platform }) => {
    const params = new URLSearchParams();
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    if (platform && platform !== 'ALL') params.set('platform', platform);
    return handleResponse(await fetch(`${BASE_URL}/api/analytics/comparison?${params.toString()}`));
  },

  getCoverage: async ({ startDate, endDate, platform }) => {
    const params = new URLSearchParams();
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    if (platform && platform !== 'ALL') params.set('platform', platform);
    return handleResponse(await fetch(`${BASE_URL}/api/analytics/coverage?${params.toString()}`));
  },

  // Recommendations
  getRecommendations: async ({ startDate, endDate, platform, status, priority, rule, isDemo = false }) => {
    if (isDemo) {
      return handleResponse(await fetch(`${BASE_URL}/api/demo/recommendations`));
    }
    const params = new URLSearchParams();
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    if (platform && platform !== 'ALL') params.set('platform', platform);
    if (status && status !== 'ALL') params.set('status', status);
    if (priority && priority !== 'ALL') params.set('priority', priority);
    if (rule && rule !== 'ALL') params.set('rule', rule);
    return handleResponse(await fetch(`${BASE_URL}/api/recommendations?${params.toString()}`));
  },

  updateRecommendationStatus: async (id, status) => {
    return handleResponse(await fetch(`${BASE_URL}/api/recommendations/${id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status })
    }));
  },

  runAnalysis: async (payload = {}) => {
    return handleResponse(await fetch(`${BASE_URL}/api/analysis/run`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }));
  },

  // Settings
  getSettings: async () => {
    return handleResponse(await fetch(`${BASE_URL}/api/settings`));
  },

  updateSettings: async (settings) => {
    return handleResponse(await fetch(`${BASE_URL}/api/settings`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(settings)
    }));
  },

  // Imports
  previewImport: async (formData) => {
    return handleResponse(await fetch(`${BASE_URL}/api/imports/preview`, {
      method: 'POST',
      body: formData
    }));
  },

  confirmImport: async (formData) => {
    return handleResponse(await fetch(`${BASE_URL}/api/imports`, {
      method: 'POST',
      body: formData
    }));
  },

  listBatches: async () => {
    return handleResponse(await fetch(`${BASE_URL}/api/imports`));
  },

  getBatch: async (batchId) => {
    return handleResponse(await fetch(`${BASE_URL}/api/imports/${batchId}`));
  },

  getBatchErrors: async (batchId, page = 0, size = 50) => {
    return handleResponse(await fetch(`${BASE_URL}/api/imports/${batchId}/errors?page=${page}&size=${size}`));
  },

  replaceBatch: async (batchId, formData) => {
    return handleResponse(await fetch(`${BASE_URL}/api/imports/${batchId}/replace`, {
      method: 'PUT',
      body: formData
    }));
  },

  deleteBatch: async (batchId) => {
    return handleResponse(await fetch(`${BASE_URL}/api/imports/${batchId}`, {
      method: 'DELETE'
    }));
  },

  // Exports URLs
  getCampaignsExportUrl: ({ startDate, endDate, platform }) => {
    const params = new URLSearchParams();
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    if (platform && platform !== 'ALL') params.set('platform', platform);
    return `${BASE_URL}/api/exports/campaigns.csv?${params.toString()}`;
  },

  getRecommendationsExportUrl: ({ startDate, endDate, platform }) => {
    const params = new URLSearchParams();
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    if (platform && platform !== 'ALL') params.set('platform', platform);
    return `${BASE_URL}/api/exports/recommendations.csv?${params.toString()}`;
  }
};
