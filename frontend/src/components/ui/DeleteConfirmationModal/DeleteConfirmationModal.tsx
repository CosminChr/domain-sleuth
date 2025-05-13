
import React from 'react';
import './DeleteConfirmationModal.css';

interface DeleteConfirmationModalProps {
    isOpen: boolean;
    itemName: string;
    onConfirm: () => void;
    onCancel: () => void;
}

const DeleteConfirmationModal: React.FC<DeleteConfirmationModalProps> = ({
                                                                             isOpen,
                                                                             itemName,
                                                                             onConfirm,
                                                                             onCancel
                                                                         }) => {
    if (!isOpen) return null;

    return (
        <div className="delete-modal-overlay" onClick={onCancel}>
            <div className="delete-modal-container" onClick={e => e.stopPropagation()}>
                <div className="delete-modal-header">
                    <h3>Confirm Deletion</h3>
                </div>
                <div className="delete-modal-content">
                    <p>Are you sure you want to delete the scan for <strong>{itemName}</strong>?</p>
                    <p className="delete-warning">This action cannot be undone.</p>
                </div>
                <div className="delete-modal-actions">
                    <button className="cancel-button" onClick={onCancel}>
                        Cancel
                    </button>
                    <button className="delete-button" onClick={onConfirm}>
                        Delete
                    </button>
                </div>
            </div>
        </div>
    );
};

export default DeleteConfirmationModal;