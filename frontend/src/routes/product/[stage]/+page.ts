import { error, redirect } from '@sveltejs/kit';
import { productStages } from '$lib/productStages';

// Only Release still lives under /product. Intake moved to /ingest and
// Preservation to /preserve; their old addresses still land there.
export const load = ({ params }: { params: { stage: string } }) => {
  const stage = productStages.find((s) => s.slug === params.stage);
  if (!stage) throw error(404, 'Not found');
  if (stage.path !== `/product/${stage.slug}`) throw redirect(307, stage.path);
  return { stage };
};
