import cx from "classnames";
import PropTypes from "prop-types";
import { memo } from "react";
import { t } from "ttag";

import RevisionMessageModal from "metabase/reference/components/RevisionMessageModal";

import S from "./EditHeader.module.css";

const EditHeader = ({
  hasRevisionHistory,
  endEditing,
  reinitializeForm = () => undefined,
  submitting,
  onSubmit,
  revisionMessageFormField,
}) => (
  <div className={cx("EditHeader wrapper py1 px3", S.editHeader)}>
    <div>{t`You are editing this page`}</div>
    <div className={S.editHeaderButtons}>
      <button
        type="button"
        className={cx(
          "Button",
          "Button--white",
          "Button--small",
          S.cancelButton,
        )}
        onClick={() => {
          endEditing();
          reinitializeForm();
        }}
      >
        {t`Cancel`}
      </button>

      {hasRevisionHistory ? (
        <RevisionMessageModal
          action={() => onSubmit()}
          field={revisionMessageFormField}
          submitting={submitting}
        >
          <button
            className={cx(
              "Button",
              "Button--primary",
              "Button--white",
              "Button--small",
              S.saveButton,
            )}
            type="button"
            disabled={submitting}
          >
            {t`Save`}
          </button>
        </RevisionMessageModal>
      ) : (
        <button
          className={cx(
            "Button",
            "Button--primary",
            "Button--white",
            "Button--small",
            S.saveButton,
          )}
          type="submit"
          disabled={submitting}
        >
          {t`Save`}
        </button>
      )}
    </div>
  </div>
);
EditHeader.propTypes = {
  hasRevisionHistory: PropTypes.bool,
  endEditing: PropTypes.func.isRequired,
  reinitializeForm: PropTypes.func,
  submitting: PropTypes.bool.isRequired,
  onSubmit: PropTypes.func,
  revisionMessageFormField: PropTypes.object,
};

export default memo(EditHeader);
