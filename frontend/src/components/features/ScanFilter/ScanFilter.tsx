import React, { useState } from 'react';
import { ScanFilterOptions, ScanStatus } from '../../../types/Scan';
import './ScanFilter.css';

interface ScanFilterProps {
    onFilter: (filters: ScanFilterOptions) => void;
    onReset: () => void;
}

const ScanFilter: React.FC<ScanFilterProps> = ({ onFilter, onReset }) => {
    const [domain, setDomain] = useState('');
    const [status, setStatus] = useState('');
    const [sinceDate, setSinceDate] = useState('');
    const [tool, setTool] = useState('');
    const [isExpanded, setIsExpanded] = useState(false);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        const filters: ScanFilterOptions = {};

        if (domain) filters.domain = domain;
        if (status) filters.status = status;
        if (sinceDate) filters.since = new Date(sinceDate);
        if (tool) filters.tool = tool;

        onFilter(filters);
    };

    const handleReset = () => {
        setDomain('');
        setStatus('');
        setSinceDate('');
        setTool('');
        onReset();
    };

    const toggleExpanded = () => {
        setIsExpanded(!isExpanded);
    };

    return (
        <div className="scan-filter">
            <div className="filter-header" onClick={toggleExpanded}>
                <h3>Filter Scans</h3>
                <span className={`expand-icon ${isExpanded ? 'expanded' : ''}`}>
                    {isExpanded ? '▼' : '▶'}
                </span>
            </div>

            {isExpanded && (
                <form onSubmit={handleSubmit} className="filter-form">
                    <div className="filter-group">
                        <label htmlFor="domain">Domain</label>
                        <input
                            id="domain"
                            type="text"
                            value={domain}
                            onChange={(e) => setDomain(e.target.value)}
                            placeholder="e.g., example.com"
                        />
                    </div>

                    <div className="filter-group">
                        <label htmlFor="status">Status</label>
                        <select
                            id="status"
                            value={status}
                            onChange={(e) => setStatus(e.target.value)}
                        >
                            <option value="">All</option>
                            <option value={ScanStatus.PENDING}>Pending</option>
                            <option value={ScanStatus.IN_PROGRESS}>In Progress</option>
                            <option value={ScanStatus.COMPLETED}>Completed</option>
                            <option value={ScanStatus.FAILED}>Failed</option>
                        </select>
                    </div>

                    <div className="filter-group">
                        <label htmlFor="sinceDate">Since Date</label>
                        <input
                            id="sinceDate"
                            type="date"
                            value={sinceDate}
                            onChange={(e) => setSinceDate(e.target.value)}
                        />
                    </div>

                    <div className="filter-group">
                        <label htmlFor="tool">Tool</label>
                        <select
                            id="tool"
                            value={tool}
                            onChange={(e) => setTool(e.target.value)}
                        >
                            <option value="">All</option>
                            <option value="Amass">Amass</option>
                        </select>
                    </div>

                    <div className="filter-actions">
                        <button type="submit" className="filter-button">Apply Filters</button>
                        <button type="button" className="reset-button" onClick={handleReset}>Reset</button>
                    </div>
                </form>
            )}
        </div>
    );
};

export default ScanFilter;