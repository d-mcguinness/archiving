import { error } from '@sveltejs/kit';
import { productStages } from '$lib/productStages';

// /product/intake, /product/preservation and /product/release share one page.
export const load = ({ params }: { params: { stage: string } }) => {
  const stage = productStages.find((s) => s.slug === params.stage);
  if (!stage) throw error(404, 'Not found');
  return { stage };
};
