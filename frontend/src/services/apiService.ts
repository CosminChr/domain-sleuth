import axios from 'axios';
import { Scan } from '../types/Scan';
import { Finding } from '../types/Finding';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

api.interceptors.request.use(
    (config) => {
        console.log(`API Request ${config.method?.toUpperCase()} ${config.url}`,
            config.data ? JSON.stringify(config.data) : 'no data');
        return config;
    },
    (error) => {
        console.error('Request error:', error);
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (response) => {
        console.log(`API Response ${response.status} from ${response.config.url}`);
        return response;
    },
    (error) => {
        console.error('Response error:', error.response ? {
            status: error.response.status,
            data: error.response.data,
            url: error.config.url
        } : error.message);
        return Promise.reject(error);
    }
);

const scanAPI = {
    // Create a new scan
    createScan: async (domain: string, config?: any): Promise<Scan> => {
        try {
            console.log(`Creating scan for domain: ${domain}${config ? ' with config' : ''}`)
            // Always include config as an empty object if not provided
            // Also include tool with default value to satisfy the backend requirement
            const response = await api.post('/scans', {
                domain,
                config: config || {},
                tool: "amass" // Default tool
            });
            return response.data;
        } catch (error) {
            console.error('Error creating scan:', error);
            throw error;
        }
    },

    // Get all scans
    getAllScans: async (): Promise<Scan[]> => {
        try {
            const response = await api.get('/scans');
            return response.data;
        } catch (error) {
            console.error('Error fetching all scans:', error);
            throw error;
        }
    },

    // Get a specific scan by ID
    getScan: async (scanId: number): Promise<Scan> => {
        try {
            const response = await api.get(`/scans/${scanId}`);
            return response.data;
        } catch (error) {
            console.error(`Error fetching scan ${scanId}:`, error);
            throw error;
        }
    },

    // Delete a scan by ID
    deleteScan: async (scanId: number): Promise<void> => {
        try {
            await api.delete(`/scans/${scanId}`);
        } catch (error) {
            console.error(`Error deleting scan ${scanId}:`, error);
            throw error;
        }
    },

    // Get all findings for a scan by ID
    getFindingsByScanId: async (scanId: number): Promise<Finding[]> => {
        try {
            const response = await api.get(`/scans/${scanId}/findings`);
            return response.data;
        } catch (error) {
            console.error(`Error fetching findings for scan ${scanId}:`, error);
            throw error;
        }
    },
};

export default scanAPI;