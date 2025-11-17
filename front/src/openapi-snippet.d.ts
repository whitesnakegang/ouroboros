declare module "openapi-snippet" {
  export interface Snippet {
    id: string;
    title: string;
    content: string;
  }

  export interface SnippetResult {
    snippets: Snippet[];
  }

  export function getEndpointSnippets(
    spec: any,
    path: string,
    method: string,
    targets?: string[]
  ): SnippetResult;
}

