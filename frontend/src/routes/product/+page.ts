import { redirect } from '@sveltejs/kit';

// The product tour is split into one page per stage; start at the first.
export const load = () => {
  throw redirect(307, '/product/intake');
};
