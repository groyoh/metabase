/* eslint-disable react/prop-types */
import cx from "classnames";
import { useState } from "react";
import { useTimeout } from "react-use";
import { t } from "ttag";

import LoadingSpinner from "metabase/components/LoadingSpinner";
import { HARD_ROW_LIMIT } from "metabase-lib/queries/utils";

import RunButtonWithTooltip from "./RunButtonWithTooltip";
import { VisualizationError } from "./VisualizationError";
import VisualizationResult from "./VisualizationResult";
import Warnings from "./Warnings";

const SLOW_MESSAGE_TIMEOUT = 4000;
const SLOWER_MESSAGE_TIMEOUT = 8000;

export default function QueryVisualization(props) {
  const {
    className,
    question,
    isRunning,
    isObjectDetail,
    isResultDirty,
    isNativeEditorOpen,
    result,
    loadingMessage,
    maxTableRows = HARD_ROW_LIMIT,
  } = props;

  const [warnings, setWarnings] = useState([]);

  return (
    <div className={cx(className, "relative stacking-context full-height")}>
      {isRunning ? (
        <VisualizationRunningState
          className="spread z2"
          loadingMessage={loadingMessage}
        />
      ) : null}
      <VisualizationDirtyState
        {...props}
        hidden={!isResultDirty || isRunning || isNativeEditorOpen}
        className="spread z2"
      />
      {!isObjectDetail && (
        <Warnings
          warnings={warnings}
          className="absolute top right mt2 mr2 z2"
          size={18}
        />
      )}
      <div
        className={cx("spread Visualization z1", {
          "Visualization--loading": isRunning,
        })}
      >
        {result?.error ? (
          <VisualizationError
            className="spread"
            error={result.error}
            via={result.via}
            question={question}
            duration={result.duration}
          />
        ) : result?.data ? (
          <VisualizationResult
            {...props}
            maxTableRows={maxTableRows}
            className="spread"
            onUpdateWarnings={setWarnings}
          />
        ) : !isRunning ? (
          <VisualizationEmptyState className="spread" />
        ) : null}
      </div>
    </div>
  );
}

export const VisualizationEmptyState = ({ className }) => (
  <div className={cx(className, "flex flex-column layout-centered text-light")}>
    <h3>{t`Here's where your results will appear`}</h3>
  </div>
);

export function VisualizationRunningState({ className = "", loadingMessage }) {
  const [isSlow] = useTimeout(SLOW_MESSAGE_TIMEOUT);
  const [isSlower] = useTimeout(SLOWER_MESSAGE_TIMEOUT);

  let message = loadingMessage;

  if (loadingMessage === "Doing science...") {
    if (isSlower()) {
      message = t`Doing science...`;
    } else if (isSlow()) {
      message = t`Waiting for results...`;
    } else {
      message = t`Talking to the database...`;
    }
  }

  return (
    <div
      className={cx(
        className,
        "Loading flex flex-column layout-centered text-brand",
      )}
    >
      <LoadingSpinner />
      <h2 className="Loading-message text-brand text-uppercase my3">
        {message}
      </h2>
    </div>
  );
}

export const VisualizationDirtyState = ({
  className,
  result,
  isRunnable,
  isRunning,
  isResultDirty,
  runQuestionQuery,
  cancelQuery,
  hidden,
}) => (
  <div
    className={cx(className, "Loading flex flex-column layout-centered", {
      "Loading--hidden pointer-events-none": hidden,
    })}
  >
    <RunButtonWithTooltip
      className="py2 px3 shadowed"
      circular
      compact
      result={result}
      hidden={!isRunnable || hidden}
      isRunning={isRunning}
      isDirty={isResultDirty}
      onRun={() => runQuestionQuery({ ignoreCache: true })}
      onCancel={() => cancelQuery()}
    />
  </div>
);
