// Shared email validation — matches backend: ^[a-zA-Z0-9]{4,20}@cuchd\.in$
export const CUCHD_RE = /^[a-zA-Z0-9]{4,20}@cuchd\.in$/i;
export const isValidEmail = (v) => CUCHD_RE.test(v.toLowerCase().trim());
