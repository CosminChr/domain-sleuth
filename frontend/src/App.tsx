import { useState, useEffect, useCallback } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import Header from './components/layout/Header/Header';
import ScanList from './components/features/ScanList/ScanList';
import ScanDetails from './components/features/ScanDetails/ScanDetails';
import ScanForm from './components/features/ScanForm/ScanForm';
import scanAPI from './services/apiService';
import { Scan } from './types/Scan';
import './App.css';

interface ScanConfig {
    verbose?: boolean;
    passive?: boolean;
    timeout?: number;
    minForRecursive?: number;
    includeNS?: boolean;
    additionalOptions?: string;
}

function App() {
    const [scans, setScans] = useState<Scan[]>([]);
    const [selectedScan, setSelectedScan] = useState<Scan | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showScanForm, setShowScanForm] = useState(false);
    const [pollingIntervals, setPollingIntervals] = useState<{ [key: number]: NodeJS.Timeout }>({});

    // Fetch all scans
    const fetchScans = useCallback(async (onlyActive = false) => {
        try {
            setIsLoading(true);
            const data = await scanAPI.getAllScans();

            // If onlyActive is true, only update scans that are in progress or pending
            if (onlyActive) {
                setScans(prevScans => {
                    // Create a map of current scans for easy lookup
                    const currentScansMap = new Map(prevScans.map(scan => [scan.id, scan]));

                    // Update the map with new data
                    data.forEach(newScan => {
                        // Always update in-progress or pending scans
                        if (newScan.status === 'IN_PROGRESS' || newScan.status === 'PENDING') {
                            currentScansMap.set(newScan.id, newScan);
                        }
                        // For completed or failed scans, only update if they weren't already completed/failed
                        else if (currentScansMap.has(newScan.id)) {
                            const currentScan = currentScansMap.get(newScan.id);
                            if (currentScan && (currentScan.status === 'IN_PROGRESS' || currentScan.status === 'PENDING')) {
                                currentScansMap.set(newScan.id, newScan);
                            }
                        }
                    });

                    return Array.from(currentScansMap.values());
                });
            } else {
                // Regular full update
                setScans(data);
            }

            setError(null);
        } catch (err) {
            console.error('Error fetching scans:', err);
            setError('Failed to load scans. Please try again.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    // Initial data loading
    useEffect(() => {
        fetchScans();

        // Set up polling for all scans (refresh every 10 seconds)
        const interval = setInterval(fetchScans, 10000);

        return () => {
            clearInterval(interval);
            // Clean up any individual scan polling
            Object.values(pollingIntervals).forEach(interval => clearInterval(interval));
        };
    }, [fetchScans, pollingIntervals]);

    // Handle scan selection
    const handleSelectScan = async (scan: Scan) => {
        try {
            // Fetch the latest data for the selected scan
            const freshScan = await scanAPI.getScan(scan.id!);
            setSelectedScan(freshScan);

            // Set up polling for this specific scan if it's not complete or failed
            if (freshScan.status === 'IN_PROGRESS' || freshScan.status === 'PENDING') {
                startPollingForScan(freshScan.id!);
            }
        } catch (err) {
            console.error('Error selecting scan:', err);
            toast.error('Failed to load scan details.');
        }
    };

    // Start polling for a specific scan
    const startPollingForScan = (scanId: number) => {
        // Clear any existing polling for this scan
        if (pollingIntervals[scanId]) {
            clearInterval(pollingIntervals[scanId]);
        }

        // Set up new polling (every 3 seconds)
        const interval = setInterval(async () => {
            try {
                const updatedScan = await scanAPI.getScan(scanId);

                // Update the scan in the list
                setScans(prevScans =>
                    prevScans.map(scan =>
                        scan.id === scanId ? updatedScan : scan
                    )
                );

                // Update selected scan if this is the one being viewed
                if (selectedScan?.id === scanId) {
                    setSelectedScan(updatedScan);
                }

                // Stop polling if the scan is complete or failed
                if (updatedScan.status === 'COMPLETED' || updatedScan.status === 'FAILED') {
                    if (pollingIntervals[scanId]) {
                        clearInterval(pollingIntervals[scanId]);
                        const newPollingIntervals = { ...pollingIntervals };
                        delete newPollingIntervals[scanId];
                        setPollingIntervals(newPollingIntervals);
                    }
                }
            } catch (err) {
                console.error(`Error polling scan ${scanId}:`, err);
            }
        }, 3000);

        setPollingIntervals(prev => ({ ...prev, [scanId]: interval }));
    };

    // Handle scan deletion
    const handleDeleteScan = async (id: number) => {
        try {
            // Stop polling for this scan first
            if (pollingIntervals[id]) {
                clearInterval(pollingIntervals[id]);
                const newPollingIntervals = { ...pollingIntervals };
                delete newPollingIntervals[id];
                setPollingIntervals(newPollingIntervals);
            }

            // Delete the scan
            await scanAPI.deleteScan(id);

            // Update UI by removing the scan from the list
            setScans(scans.filter(scan => scan.id !== id));

            // If the deleted scan was selected, clear the selection
            if (selectedScan?.id === id) {
                setSelectedScan(null);
            }

            toast.success('Scan deleted successfully');
        } catch (err) {
            console.error('Error deleting scan:', err);

            // Type assertion for the error object
            const error = err as { response?: { status: number } };

            // Show more specific error message if available
            if (error.response && error.response.status === 404) {
                toast.error('Scan not found. It may have been already deleted.');

                // Still update the UI to remove the scan
                setScans(scans.filter(scan => scan.id !== id));
                if (selectedScan?.id === id) {
                    setSelectedScan(null);
                }
            } else {
                toast.error('Failed to delete scan. Please try again.');
            }
        }
    };

    // Handle creating a new scan
    const handleCreateScan = async (domain: string, config?: ScanConfig) => {
        try {
            const newScan = await scanAPI.createScan(domain, config);

            // Update the scans list
            setScans(prev => [newScan, ...prev]);

            // Select the new scan
            setSelectedScan(newScan);

            // Start polling for this scan
            startPollingForScan(newScan.id!);

            // Close the form
            setShowScanForm(false);

            toast.success(`Scan created for ${domain}`);
        } catch (err) {
            console.error('Error creating scan:', err);
            toast.error('Failed to create scan. Please try again.');
            throw err;
        }
    };

    return (
        <div className="app-container">
            <Header />
            <div className="app-content">
                <div className="sidebar">
                    <ScanList
                        scans={scans}
                        isLoading={isLoading}
                        error={error}
                        onSelect={handleSelectScan}
                        onDelete={handleDeleteScan}
                        onCreateScan={() => setShowScanForm(true)}
                    />
                </div>

                <div className="main-content">
                    <div className="main-content-inner">
                        {selectedScan ? (
                            <div className="detail-content">
                                <ScanDetails scan={selectedScan} />
                            </div>
                        ) : (
                            <div className="empty-state">
                                <h2>No Scan Selected</h2>
                                <p>Select a scan from the list or create a new one to view details.</p>
                                <button
                                    className="create-scan-button"
                                    onClick={() => setShowScanForm(true)}
                                >
                                    Create New Scan
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {showScanForm && (
                <ScanForm
                    onSubmit={handleCreateScan}
                    onCancel={() => setShowScanForm(false)}
                />
            )}

            <ToastContainer position="bottom-right" />
        </div>
    );
}

export default App;