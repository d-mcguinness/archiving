import { error, redirect } from '@sveltejs/kit';
import { productStages } from '$lib/productStages';

// /product/preservation and /product/release share one page. Intake moved to
// /ingest; its old address still lands there.
export const load = ({ params }: { params: { stage: string } }) => {
  const stage = productStages.find((s) => s.slug === params.stage);
  if (!stage) throw error(404, 'Not found');
  if (stage.path !== `/product/${stage.slug}`) throw redirect(307, stage.path);
  return { stage };
};
