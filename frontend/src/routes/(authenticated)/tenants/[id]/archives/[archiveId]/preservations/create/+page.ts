import type { PageLoad } from './$types';

export const load: PageLoad = ({ params }) => {
  return {
    tenantId: params.id,
    archiveId: params.archiveId
  };
};
