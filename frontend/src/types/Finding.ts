export interface Finding {
    id: number;
    scanId: number;
    type: string;
    value: string;
    ipAddress?: string;
    cnameValue?: string;
    createdAt: string;
}

export enum FindingType {
    SUBDOMAIN = "subdomain",
    NAMESERVER = "nameserver",
    MX_RECORD = "mx_record",
    CNAME = "cname",
    RESULT = "result"
}

export interface FindingCounts {
    total: number;
    subdomain: number;
    nameserver: number;
    mx_record: number;
    cname: number;
    result: number;
}
