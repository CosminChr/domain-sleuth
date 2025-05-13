export enum ScanStatus {
    PENDING = "PENDING",
    IN_PROGRESS = "IN_PROGRESS",
    COMPLETED = "COMPLETED",
    FAILED = "FAILED"
}

export interface Scan {
    id?: number;
    domain: string;
    status: ScanStatus;
    tool?: string;
    startTime?: string;
    endTime?: string;
    findings?: string[] | any[];
    errorMessage?: string;
    config?: any;
}

export interface ScanLog {
    id: number;
    scanId: number;
    timestamp: string;
    content: string;
    level: string;
}

export interface ScanHistoryEntry {
    id: number;
    scanId?: number;
    timestamp: string;
    status: string;
    message?: string;
}

export interface ScanFilterOptions {
    domain?: string;
    status?: string;
    since?: Date;
    tool?: string;
}