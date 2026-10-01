export function validSelection(value: unknown): value is number[] {
  return Array.isArray(value) && value.length > 0 && value.length <= 50 && value.every(id => Number.isSafeInteger(id) && id > 0);
}
export function cleanTasks(value: unknown, courses: number[]) {
  if (!Array.isArray(value)) return [];
  return value.filter(item => item && typeof item.title === "string" && item.title.trim()).slice(0, 12).map(item => ({
    title: item.title.slice(0, 200), notes: typeof item.notes === "string" ? item.notes.slice(0, 1600) : "", kind: String(item.kind ?? "과제").slice(0, 30),
    course_id: courses.includes(item.course_id) ? item.course_id : null,
    deadline: typeof item.deadline === "string" && /^\d{4}-\d{2}-\d{2}$/.test(item.deadline) ? item.deadline : null,
  })).sort((a, b) => (a.deadline ?? "9999").localeCompare(b.deadline ?? "9999"));
}
export type Opportunity = { id: string; kind: string; title: string; detail: string; deadline: string | null; starts_on: string | null; url: string | null; eligibility: string; days_left: number | null };
export function normalizeOpportunities(contests: any[], scholarships: any[], academic: any[]): Opportunity[] {
  return [
    ...contests.map(x => ({ ...x, id: `contest:${x.id}`, kind: "공모전", eligibility: x.target, starts_on: x.start_date })),
    ...scholarships.map(x => ({ ...x, id: `scholarship:${x.id}`, kind: "장학금", starts_on: x.application_start })),
    ...academic.map(x => ({ ...x, id: `academic:${x.id}`, kind: "학사공지", deadline: x.end_date ?? x.start_date, starts_on: x.start_date })),
  ].map(x => ({ id: x.id, kind: x.kind, title: String(x.title ?? "").slice(0, 250), detail: String(x.description ?? "").slice(0, 3000), deadline: x.deadline ?? null,
    starts_on: x.starts_on ?? null, url: x.application_url ?? x.source_url ?? null, eligibility: String(x.eligibility ?? "").slice(0, 2000), days_left: null }));
}
export function rankOpportunities(items: Opportunity[], courses: { name: string; department?: string | null }[], today: string): Opportunity[] {
  const current = Date.parse(`${today}T00:00:00Z`);
  const terms = courses.flatMap(course => `${course.name} ${course.department ?? ""}`.split(/[\s·()\d]+/)).filter(term => term.length >= 2 && !["개론", "이해", "학부", "기초"].includes(term));
  return items.map(item => ({ ...item, days_left: item.deadline ? Math.round((Date.parse(`${item.deadline.slice(0, 10)}T00:00:00Z`) - current) / 86400000) : null }))
    .filter(item => item.days_left === null || Number.isFinite(item.days_left) && item.days_left >= 0)
    .sort((a, b) => {
      const score = (item: Opportunity) => (item.days_left === null ? -30 : 40 - Math.min(item.days_left, 90)) +
        terms.filter(term => `${item.title} ${item.detail} ${item.eligibility}`.includes(term)).length * 8;
      return score(b) - score(a);
    });
}
