import React, { useState } from 'react';
import ScanCard from '../ScanCard/ScanCard';
import { Scan, ScanStatus } from '../../../types/Scan';
import './ScanList.css';

interface ScanListProps {
    scans: Scan[];
    isLoading: boolean;
    error: string | null;
    onSelect: (scan: Scan) => void;
    onDelete: (id: number) => void;
    onCreateScan: () => void;
}

const ScanList: React.FC<ScanListProps> = ({
                                               scans,
                                               isLoading,
                                               error,
                                               onSelect,
                                               onDelete,
                                               onCreateScan
                                           }) => {
    const [selectedScanId, setSelectedScanId] = useState<number | null>(null);
    const [filterStatus, setFilterStatus] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState<string>('');

    const handleSelectScan = (scan: Scan) => {
        setSelectedScanId(scan.id || null);
        onSelect(scan);
    };

    const handleDelete = (id: number) => {
        onDelete(id);
    };

    const filteredScans = scans.filter(scan => {
        // Apply status filter if set
        if (filterStatus && scan.status !== filterStatus) {
            return false;
        }

        // Apply search filter if there's a search term
        if (searchTerm.trim()) {
            return scan.domain.toLowerCase().includes(searchTerm.toLowerCase());
        }

        return true;
    });

    const statusCounts = scans.reduce((counts, scan) => {
        counts[scan.status] = (counts[scan.status] || 0) + 1;
        return counts;
    }, {} as Record<string, number>);

    return (
        <div className="scan-list">
            <div className="scan-list-header">
                <h2>Scans</h2>
                <button className="create-scan-button" onClick={onCreateScan}>
                    New Scan
                </button>
            </div>

            <div className="scan-list-search">
                <input
                    type="text"
                    placeholder="Search domains..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                />
            </div>

            <div className="scan-list-filters">
                <button
                    className={`filter-button ${filterStatus === null ? 'active' : ''}`}
                    onClick={() => setFilterStatus(null)}
                >
                    All ({scans.length})
                </button>
                {Object.entries(ScanStatus).map(([key, value]) => (
                    <button
                        key={key}
                        className={`filter-button ${filterStatus === value ? 'active' : ''}`}
                        onClick={() => setFilterStatus(value)}
                    >
                        {key} ({statusCounts[value] || 0})
                    </button>
                ))}
            </div>

            <div className="scan-list-content">
                {isLoading ? (
                    <div className="scan-list-loading">Loading scans...</div>
                ) : error ? (
                    <div className="scan-list-error">{error}</div>
                ) : filteredScans.length === 0 ? (
                    <div className="scan-list-empty">
                        {searchTerm || filterStatus ? (
                            <p>No scans match your filters.</p>
                        ) : (
                            <p>No scans found. Create a new scan to get started.</p>
                        )}
                    </div>
                ) : (
                    filteredScans.map(scan => (
                        <ScanCard
                            key={scan.id}
                            scan={scan}
                            isSelected={scan.id === selectedScanId}
                            onSelect={() => handleSelectScan(scan)}
                            onDelete={() => handleDelete(scan.id!)}
                        />
                    ))
                )}
            </div>
        </div>
    );
};

export default ScanList;