export function sendRequest<T>(url: string): Promise<T> {
  return fetch(url)
    .then(catchNotSuccessfulResponse)
    .then((res) => res.json());
}

function catchNotSuccessfulResponse(response: Response): Response {
  if (response.ok) {
    return response;
  }

  throw new Error(`Request failed with status: ${response.status}`);
}
