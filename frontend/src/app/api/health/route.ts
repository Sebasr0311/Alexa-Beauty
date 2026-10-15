export const dynamic = 'force-dynamic';

export async function GET() {
  return Response.json(
    {
      status: 'UP',
      service: 'alexa-beauty-frontend',
      timestamp: new Date().toISOString(),
    },
    { status: 200 }
  );
}

export async function HEAD() {
  return new Response(null, { status: 200 });
}
