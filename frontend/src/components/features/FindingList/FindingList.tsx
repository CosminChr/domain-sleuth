import React, { useState, useEffect, useMemo } from 'react';
import scanAPI from '../../../services/apiService';
import { Finding, FindingType, FindingCounts } from '../../../types/Finding';
import { formatLocalDate } from '../../../utils/dateUtils';
import './FindingList.css';

interface FindingListProps {
    scanId: number;
}

const FindingList: React.FC<FindingListProps> = ({ scanId }) => {
    const [findings, setFindings] = useState<Finding[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState<string>('');
    const [activeFilterType, setActiveFilterType] = useState<FindingType | null>(null);
    const [findingCounts, setFindingCounts] = useState<FindingCounts>({
        total: 0,
        subdomain: 0,
        nameserver: 0,
        mx_record: 0,
        cname: 0,
        result: 0,
    });

    useEffect(() => {
        const fetchFindings = async () => {
            setIsLoading(true);
            setError(null);
            try {
                const data: Finding[] = await scanAPI.getFindingsByScanId(scanId);
                setFindings(data);

                // Calculate counts based on the remaining types
                const counts: FindingCounts = { total: data.length, subdomain: 0, nameserver: 0, mx_record: 0, cname: 0, result: 0 };
                data.forEach((finding: Finding) => {
                    if (finding.type === FindingType.SUBDOMAIN) counts.subdomain++;
                    else if (finding.type === FindingType.NAMESERVER) counts.nameserver++;
                    else if (finding.type === FindingType.MX_RECORD) counts.mx_record++;
                    else if (finding.type === FindingType.CNAME) counts.cname++;
                    else if (finding.type === FindingType.RESULT) counts.result++;
                });
                setFindingCounts(counts);

            } catch (err) {
                console.error('Error fetching findings:', err);
                setError('Failed to load findings. Please try again later.');
            } finally {
                setIsLoading(false);
            }
        };

        if (scanId) {
            fetchFindings();
        }
    }, [scanId]);

    const filteredFindings = useMemo(() => {
        return findings.filter((finding: Finding) => {
            const matchesType = activeFilterType ? finding.type === activeFilterType : true;
            const matchesSearch = searchTerm
                ? finding.value.toLowerCase().includes(searchTerm.toLowerCase()) ||
                finding.type.toLowerCase().includes(searchTerm.toLowerCase()) ||
                (finding.ipAddress?.toLowerCase() || '').includes(searchTerm.toLowerCase()) ||
                (finding.cnameValue?.toLowerCase() || '').includes(searchTerm.toLowerCase()) // Include CNAME value in search
                : true;
            return matchesType && matchesSearch;
        });
    }, [findings, activeFilterType, searchTerm]);

    const formatDate = (dateString: string) => {
        return formatLocalDate(dateString);
    };

    const getFindingTypeClass = (type: string) => {
        switch (type) {
            case FindingType.SUBDOMAIN:
                return 'finding-type-subdomain';
            case FindingType.NAMESERVER:
                return 'finding-type-nameserver';
            case FindingType.MX_RECORD:
                return 'finding-type-mx_record';
            case FindingType.CNAME:
                return 'finding-type-cname';
            case FindingType.RESULT:
                return 'finding-type-result';
            default:
                return '';
        }
    };

    if (isLoading) {
        return <div className="finding-list-loading">Loading findings...</div>;
    }

    if (error) {
        return <div className="finding-list-error">{error}</div>;
    }

    return (
        <div className="finding-list">
            <div className="finding-list-header">
                <h2 style={{ paddingLeft: '20px' }}>Findings <span className="finding-count-badge">{filteredFindings.length} / {findingCounts.total}</span></h2>
                <div className="finding-type-filters">
                    <button
                        className={`type-button ${activeFilterType === null ? 'active' : ''}`}
                        onClick={() => setActiveFilterType(null)}
                    >
                        All ({findingCounts.total})
                    </button>
                    <button
                        className={`type-button ${activeFilterType === FindingType.SUBDOMAIN ? 'active' : ''}`}
                        onClick={() => setActiveFilterType(FindingType.SUBDOMAIN)}
                    >
                        Subdomains ({findingCounts.subdomain})
                    </button>
                    <button
                        className={`type-button ${activeFilterType === FindingType.NAMESERVER ? 'active' : ''}`}
                        onClick={() => setActiveFilterType(FindingType.NAMESERVER)}
                    >
                        Nameservers ({findingCounts.nameserver})
                    </button>
                    <button
                        className={`type-button ${activeFilterType === FindingType.MX_RECORD ? 'active' : ''}`}
                        onClick={() => setActiveFilterType(FindingType.MX_RECORD)}
                    >
                        MX Records ({findingCounts.mx_record})
                    </button>
                    <button
                        className={`type-button ${activeFilterType === FindingType.CNAME ? 'active' : ''}`}
                        onClick={() => setActiveFilterType(FindingType.CNAME)}
                    >
                        CNAME Records ({findingCounts.cname})
                    </button>
                    <button
                        className={`type-button ${activeFilterType === FindingType.RESULT ? 'active' : ''}`}
                        onClick={() => setActiveFilterType(FindingType.RESULT)}
                    >
                        Results ({findingCounts.result})
                    </button>
                </div>
            </div>

            <div className="finding-search">
                <input
                    type="text"
                    placeholder="Search findings by name, value, type, or IP Address..." // Updated placeholder
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                />
            </div>

            {filteredFindings.length === 0 ? (
                <div className="no-findings-message">
                    {findings.length === 0 ? 'No findings were reported for this scan.' : 'No findings match your current filters.'}
                </div>
            ) : (
                <div className="findings-table-container">
                    <table className="findings-table">
                        <thead>
                        <tr>
                            <th>Type</th>
                            <th>Name</th>
                            <th>Value</th>
                            <th>IP Address</th>
                            <th>Discovered</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredFindings.map((finding) => (
                            <tr key={finding.id} className={`finding-item ${getFindingTypeClass(finding.type)}`}>
                                <td>
                                        <span className="finding-type">
                                            {finding.type.replace('_', ' ')}
                                        </span>
                                </td>
                                <td className="finding-value">{finding.value}</td>
                                <td className="finding-cname-value">{finding.type === FindingType.CNAME ? finding.cnameValue : 'N/A'}</td>
                                <td className="finding-ip-address">{finding.ipAddress || 'N/A'}</td>
                                <td className="finding-date">{formatDate(finding.createdAt)}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default FindingList;