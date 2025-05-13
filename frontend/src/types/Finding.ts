export interface Finding {
    id: number;
    scanId: number;
    type: string;
    value: string;
    ipAddress?: string;
    createdAt: string;
}

export enum FindingType {
    SUBDOMAIN = "subdomain",
    NAMESERVER = "nameserver",
    MX_RECORD = "mx_record",
    RESULT = "result"
}

export interface FindingCounts {
    total: number;
    subdomain: number;
    nameserver: number;
    mx_record: number;
    result: number;
}
