/**
 * Formats a date string to the user's local timezone
 * @param dateString - ISO date string or any valid date string
 * @returns Formatted date string in the user's local timezone
 */
export const formatLocalDate = (dateString?: string): string => {
  if (!dateString) return 'N/A';
  
  const date = new Date(dateString);
  
  // Check if the date is valid
  if (isNaN(date.getTime())) return 'Invalid date';
  
  // Format the date using the user's locale and timezone
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric',
    hour12: true,
    timeZoneName: 'short'
  });
};
