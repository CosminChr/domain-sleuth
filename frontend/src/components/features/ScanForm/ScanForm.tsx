import React, { useState } from 'react';
import { toast } from 'react-toastify';
import './ScanForm.css';

interface ScanConfig {
    verbose?: boolean;
    passive?: boolean;
    timeout?: number;
    minForRecursive?: number;
    includeNS?: boolean;
    additionalOptions?: string;
}

interface ScanFormProps {
    onSubmit: (domain: string, config?: ScanConfig) => Promise<void>;
    onCancel: () => void;
}

const ScanForm: React.FC<ScanFormProps> = ({ onSubmit, onCancel }) => {
    const [domain, setDomain] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [showAdvancedOptions, setShowAdvancedOptions] = useState(false);

    // Amass configuration options
    const [verbose, setVerbose] = useState(true);
    const [passive, setPassive] = useState(true);
    const [timeout, setTimeout] = useState(5);
    const [minForRecursive, setMinForRecursive] = useState(2);
    const [includeNS, setIncludeNS] = useState(false);
    const [additionalOptions, setAdditionalOptions] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // Validate domain - fixed regex
        const domainRegex = /^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}$/;
        if (!domainRegex.test(domain)) {
            toast.error('Please enter a valid domain name (e.g., example.com)');
            return;
        }

        setIsSubmitting(true);

        // Build Amass configuration
        const config: ScanConfig | undefined = showAdvancedOptions ? {
            verbose,
            passive,
            timeout,
            minForRecursive,
            includeNS,
            additionalOptions: additionalOptions.trim()
        } : undefined;

        try {
            await onSubmit(domain, config);
        } catch (error) {
            console.error('Error submitting scan:', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="scan-form-overlay">
            <div className="scan-form-container">
                <div className="scan-form-header">
                    <h2>New Scan</h2>
                    <button
                        className="close-button"
                        onClick={onCancel}
                        type="button"
                    >
                        ×
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="scan-form">
                    <div className="form-group">
                        <label htmlFor="domain">Domain to scan:</label>
                        <input
                            id="domain"
                            type="text"
                            value={domain}
                            onChange={(e) => setDomain(e.target.value)}
                            placeholder="e.g., example.com"
                            required
                            disabled={isSubmitting}
                            autoFocus
                        />
                        <small>Enter a valid domain name without protocol (http/https)</small>
                    </div>

                    <div className="advanced-options-toggle">
                        <button
                            type="button"
                            onClick={() => setShowAdvancedOptions(!showAdvancedOptions)}
                            className="toggle-button"
                        >
                            {showAdvancedOptions ? 'Hide Advanced Options' : 'Show Advanced Options'}
                        </button>
                    </div>

                    {showAdvancedOptions && (
                        <div className="advanced-options">
                            <h3>Amass Configuration</h3>

                            <div className="form-row">
                                <div className="form-group checkbox">
                                    <input
                                        id="verbose"
                                        type="checkbox"
                                        checked={verbose}
                                        onChange={(e) => setVerbose(e.target.checked)}
                                        disabled={isSubmitting}
                                    />
                                    <label htmlFor="verbose">Verbose output</label>
                                </div>

                                <div className="form-group checkbox">
                                    <input
                                        id="passive"
                                        type="checkbox"
                                        checked={passive}
                                        onChange={(e) => setPassive(e.target.checked)}
                                        disabled={isSubmitting}
                                    />
                                    <label htmlFor="passive">Passive mode only</label>
                                </div>
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label htmlFor="timeout">Timeout (minutes):</label>
                                    <input
                                        id="timeout"
                                        type="number"
                                        min="1"
                                        max="60"
                                        value={timeout}
                                        onChange={(e) => setTimeout(parseInt(e.target.value) || 5)}
                                        disabled={isSubmitting}
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="minForRecursive">Min for recursive:</label>
                                    <input
                                        id="minForRecursive"
                                        type="number"
                                        min="1"
                                        max="10"
                                        value={minForRecursive}
                                        onChange={(e) => setMinForRecursive(parseInt(e.target.value) || 2)}
                                        disabled={isSubmitting}
                                    />
                                </div>
                            </div>

                            <div className="form-row">
                                <div className="form-group checkbox">
                                    <input
                                        id="includeNS"
                                        type="checkbox"
                                        checked={includeNS}
                                        onChange={(e) => setIncludeNS(e.target.checked)}
                                        disabled={isSubmitting}
                                    />
                                    <label htmlFor="includeNS">Include unresolvable</label>
                                </div>
                            </div>
                            <div className="form-group">
                                <label htmlFor="additionalOptions">Additional options:</label>
                                <input
                                    id="additionalOptions"
                                    type="text"
                                    value={additionalOptions}
                                    onChange={(e) => setAdditionalOptions(e.target.value)}
                                    placeholder="e.g., -rf resolvers.txt -bl blacklist.txt"
                                    disabled={isSubmitting}
                                />
                                <small>Additional Amass command-line options</small>
                            </div>
                        </div>
                    )}

                    <div className="form-actions">
                        <button
                            type="submit"
                            className="submit-button"
                            disabled={isSubmitting || !domain.trim()}
                        >
                            {isSubmitting ? 'Creating...' : 'Start Scan'}
                        </button>
                        <button
                            type="button"
                            className="cancel-button"
                            onClick={onCancel}
                            disabled={isSubmitting}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default ScanForm;