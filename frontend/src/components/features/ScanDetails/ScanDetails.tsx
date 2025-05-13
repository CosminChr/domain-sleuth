import React, { useState, useEffect } from 'react';
import { Scan } from '../../../types/Scan';
import FindingList from '../FindingList/FindingList';
import scanAPI from '../../../services/apiService';
import { formatLocalDate } from '../../../utils/dateUtils';
import './ScanDetails.css';

interface ScanDetailsProps {
    scan: Scan;
}

// Create an extended interface to properly type check the summary property
interface ExtendedScan extends Scan {
    summary?: string;
}

const ScanDetails: React.FC<ScanDetailsProps> = ({ scan: initialScan }) => {
    // Add a local state for the scan to ensure it can update independently
    const [scan, setScan] = useState<Scan>(initialScan);
    const [activeTab, setActiveTab] = useState<'info' | 'findings'>('info');
    const [findingsCount, setFindingsCount] = useState<number>(scan.findings?.length || 0);
    const [isPolling, setIsPolling] = useState<boolean>(
        scan.status === 'IN_PROGRESS' || scan.status === 'PENDING'
    );

    // Set up polling for the current scan
    useEffect(() => {
        // Immediately update local scan state when initialScan changes
        setScan(initialScan);

        // Only set up polling if scan is in progress or pending
        if (initialScan.status !== 'COMPLETED' && initialScan.status !== 'FAILED' && initialScan.id) {
            const interval = setInterval(async () => {
                try {
                    // Fetch the latest scan data directly from the API
                    const freshScan = await scanAPI.getScan(initialScan.id!);
                    console.log('ScanDetails - Fresh scan data:', freshScan);
                    setScan(freshScan);

                    // If scan is complete or failed, stop polling
                    if (freshScan.status === 'COMPLETED' || freshScan.status === 'FAILED') {
                        clearInterval(interval);
                    }
                } catch (error) {
                    console.error('Error fetching scan update:', error);
                }
            }, 2000); // Poll every 2 seconds

            // Clean up interval on unmount or when initialScan changes
            return () => clearInterval(interval);
        }
    }, [initialScan.id, initialScan]);

    // Update findings count and polling status when scan changes
    useEffect(() => {
        setFindingsCount(scan.findings?.length || 0);
        // Only set polling to true if the scan is still in progress or pending
        setIsPolling(scan.status === 'IN_PROGRESS' || scan.status === 'PENDING');
    }, [scan]); // Simplified dependencies: effect runs when 'scan' object changes

    // Cast to ExtendedScan to allow access to the summary property
    const extendedScan = scan as ExtendedScan;

    const formatDate = (date: string | undefined) => {
        return formatLocalDate(date);
    };

    const getStatusClass = () => {
        switch (scan.status) {
            case 'COMPLETED': return 'status-completed';
            case 'IN_PROGRESS': return 'status-in-progress';
            case 'PENDING': return 'status-pending';
            case 'FAILED': return 'status-failed';
            default: return '';
        }
    };

    return (
        <div className="scan-details-container">
            <div className="scan-details-header">
                <div className="scan-details-title">
                    <h2>{scan.domain}</h2>
                    <div className="scan-meta">
                        <span className={`scan-status ${getStatusClass()}`}>
                            {scan.status.toLowerCase().replace('_', ' ')}
                            {isPolling && <span className="polling-indicator"></span>}
                        </span>
                        <span className="scan-tool">{scan.tool || 'Amass'}</span>
                    </div>
                </div>
            </div>

            <div className="scan-details-tabs">
                <button
                    className={`tab-button ${activeTab === 'info' ? 'active' : ''}`}
                    onClick={() => setActiveTab('info')}
                >
                    Information
                </button>
                <button
                    className={`tab-button ${activeTab === 'findings' ? 'active' : ''}`}
                    onClick={() => setActiveTab('findings')}
                >
                    Findings ({findingsCount})
                    {isPolling && <span className="polling-indicator-small"></span>}
                </button>
            </div>

            <div className="scan-details-content">
                {activeTab === 'info' && (
                    <div className="scan-details-info">
                        <div className="details-section">
                            <h3>Scan Details</h3>
                            <div className="details-row">
                                <div className="details-label">Domain</div>
                                <div className="details-value domain-value">{scan.domain}</div>
                            </div>
                            <div className="details-row">
                                <div className="details-label">Status</div>
                                <div className="details-value status-value">
                                    <span className={getStatusClass()}>
                                        {scan.status.toLowerCase().replace('_', ' ')}
                                        {isPolling && <span className="polling-indicator-small"></span>}
                                    </span>
                                </div>
                            </div>
                            <div className="details-row">
                                <div className="details-label">Tool</div>
                                <div className="details-value">{scan.tool || 'Amass'}</div>
                            </div>
                            <div className="details-row">
                                <div className="details-label">Start Time</div>
                                <div className="details-value">{formatDate(scan.startTime)}</div>
                            </div>
                            <div className="details-row">
                                <div className="details-label">End Time</div>
                                <div className="details-value">{formatDate(scan.endTime)}</div>
                            </div>
                            {extendedScan.summary && (
                                <div className="details-row">
                                    <div className="details-label">Summary</div>
                                    <div className="details-value">{extendedScan.summary}</div>
                                </div>
                            )}
                            {scan.errorMessage && (
                                <div className="details-row">
                                    <div className="details-label">Error</div>
                                    <div className="details-value">
                                        <div className="error-message">{scan.errorMessage}</div>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {activeTab === 'findings' && (
                    <div className="scan-details-findings">
                        {scan.id ? (
                            <FindingList scanId={scan.id} key={`findings-${scan.id}-${scan.status}-${findingsCount}`} />
                        ) : (
                            <div className="no-findings">
                                {scan.status === 'COMPLETED'
                                    ? 'No findings available for this scan.'
                                    : 'Findings will appear here once the scan is complete.'}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default ScanDetails;