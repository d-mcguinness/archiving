import { redirect } from '@sveltejs/kit';
import { productStages } from '$lib/productStages';

// The product tour is one page per stage; start at the first.
export const load = () => {
  throw redirect(307, productStages[0].path);
};
