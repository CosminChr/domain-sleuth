import React, { useState } from 'react';
import { Scan, ScanStatus } from '../../../types/Scan';
import DeleteConfirmationModal from "../../ui/DeleteConfirmationModal/DeleteConfirmationModal.tsx";
import { formatLocalDate } from '../../../utils/dateUtils';
import './ScanCard.css';

interface ScanCardProps {
    scan: Scan;
    isSelected: boolean;
    onSelect: () => void;
    onDelete: () => void;
}

const ScanCard: React.FC<ScanCardProps> = ({ scan, isSelected, onSelect, onDelete }) => {
    const [showDeleteModal, setShowDeleteModal] = useState(false);

    // Format date for display
    const formatDate = (dateString?: string) => {
        return formatLocalDate(dateString);
    };

    // Get status class for styling
    const getStatusClass = () => {
        switch (scan.status) {
            case ScanStatus.IN_PROGRESS:
                return 'status-in-progress';
            case ScanStatus.COMPLETED:
                return 'status-completed';
            case ScanStatus.FAILED:
                return 'status-failed';
            case ScanStatus.PENDING:
                return 'status-pending';
            default:
                return '';
        }
    };

    const handleDeleteClick = (e: React.MouseEvent) => {
        e.stopPropagation(); // Prevent triggering onSelect
        setShowDeleteModal(true);
    };

    const confirmDelete = () => {
        setShowDeleteModal(false);
        onDelete();
    };

    const cancelDelete = () => {
        setShowDeleteModal(false);
    };

    return (
        <>
            <div
                className={`scan-card ${isSelected ? 'selected' : ''}`}
                onClick={onSelect}
            >
                <div className="scan-card-header">
                    <span className="domain-name">{scan.domain}</span>
                    <span className={`scan-status ${getStatusClass()}`}>{scan.status}</span>
                </div>

                <div className="scan-card-content">
                    <div className="scan-info">
                        <span className="info-label">Tool:</span>
                        <span className="info-value">{scan.tool || 'Amass'}</span>
                    </div>

                    <div className="scan-info">
                        <span className="info-label">Started:</span>
                        <span className="info-value">{formatDate(scan.startTime)}</span>
                    </div>

                    {scan.status === ScanStatus.COMPLETED && (
                        <div className="scan-info">
                            <span className="info-label">Findings:</span>
                            <span className="info-value">
                                {scan.findings && scan.findings.length > 0
                                    ? `${scan.findings.length} items`
                                    : 'No findings'}
                            </span>
                        </div>
                    )}

                    {scan.status === ScanStatus.FAILED && (
                        <div className="scan-info error">
                            <span className="info-value">{scan.errorMessage || 'An error occurred'}</span>
                        </div>
                    )}
                </div>

                <div className="scan-card-actions">
                    <button
                        className="delete-button"
                        onClick={handleDeleteClick}
                        aria-label="Delete scan"
                    >
                        Delete
                    </button>
                </div>
            </div>

            <DeleteConfirmationModal
                isOpen={showDeleteModal}
                itemName={scan.domain}
                onConfirm={confirmDelete}
                onCancel={cancelDelete}
            />
        </>
    );
};

export default ScanCard;