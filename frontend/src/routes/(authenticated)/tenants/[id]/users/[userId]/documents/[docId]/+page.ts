import type { PageLoad } from './$types';

export const load: PageLoad = ({ params }) => {
  return {
    tenantId: params.id,
    userId: params.userId,
    docId: params.docId
  };
};
