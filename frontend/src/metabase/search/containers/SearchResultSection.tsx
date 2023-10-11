import { t } from "ttag";
import type { WrappedResult } from "metabase/search/types";
import { SearchResult } from "metabase/search/components/SearchResult";
import { Paper, Stack, Text } from "metabase/ui";

export const SearchResultSection = ({
  items,
  totalResults,
}: {
  items: WrappedResult[];
  totalResults: number;
}) => {
  const resultsLabel = totalResults === 1 ? t`result` : t`results`;
  return (
    <Paper px="sm" py="md">
      <Stack spacing="sm">
        <Text tt="uppercase" fw={700} ml="sm" mb="sm">
          {totalResults} {resultsLabel}
        </Text>
        {items.map(item => {
          return (
            <SearchResult key={`${item.id}__${item.model}`} result={item} />
          );
        })}
      </Stack>
    </Paper>
  );
};