// Load function for update archive page
// Receives params with the dynamic [id] parameter
export const load = async ({ params }) => {
  return {
    id: params.id
  };
};
