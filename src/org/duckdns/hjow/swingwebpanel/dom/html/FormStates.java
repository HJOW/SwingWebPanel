package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The form related selector states of the elements that the form layer implements (HTML Standard 4.16.3 "Pseudo-classes"): <code>:checked</code>,
 * <code>:disabled</code>, <code>:enabled</code>, <code>:indeterminate</code>, <code>:default</code>, <code>:required</code>, <code>:optional</code>,
 * <code>:read-write</code>, <code>:valid</code>, <code>:invalid</code>, <code>:in-range</code>, <code>:out-of-range</code> and
 * <code>:placeholder-shown</code>. Every method answers from the live state of the control (the typed value, the checkedness, the selectedness, the
 * validity), not from the content attributes. A method returns null for an element that the form layer does not implement, so that the selector engine falls
 * back to the content attributes.
 * <p>
 * 폼 계층이 구현하는 요소의 폼 관련 선택자 상태(HTML 표준 4.16.3 "Pseudo-classes"): <code>:checked</code>, <code>:disabled</code>, <code>:enabled</code>,
 * <code>:indeterminate</code>, <code>:default</code>, <code>:required</code>, <code>:optional</code>, <code>:read-write</code>, <code>:valid</code>,
 * <code>:invalid</code>, <code>:in-range</code>, <code>:out-of-range</code>, <code>:placeholder-shown</code>. 모든 메서드는 콘텐츠 속성이 아니라 컨트롤의 실제 상태(입력한 값,
 * checkedness, selectedness, 유효성)로 답한다. 폼 계층이 구현하지 않는 요소에는 null 을 반환해, 선택자 엔진이 콘텐츠 속성으로 대신하게 한다.
 */
public final class FormStates {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FormStates() {
    }

    /**
     * Converts a boolean to a {@link Boolean}.
     * <p>
     * boolean 을 {@link Boolean} 으로 바꾼다.
     *
     * @param b the value / 값
     * @return the object / 객체
     */
    private static Boolean of(boolean b) {
        return b ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * <code>:checked</code>: a checkbox or radio button whose checkedness is true, an option whose selectedness is true.
     * <p>
     * <code>:checked</code>: checkedness 가 true 인 체크박스나 라디오 버튼, selectedness 가 true 인 option.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean checked(Element e) {
        if (e instanceof HtmlInputElement) {
            HtmlInputElement in = (HtmlInputElement) e;
            return of((in.kind() == InputKinds.CHECKBOX || in.kind() == InputKinds.RADIO) && in.isChecked());
        } else if (e instanceof HtmlOptionElement) {
            return of(((HtmlOptionElement) e).isSelected());
        }
        return null;
    }

    /**
     * <code>:disabled</code>: a form control that is disabled (including the effect of a disabled fieldset), an option or optgroup that is disabled.
     * <p>
     * <code>:disabled</code>: 비활성 폼 컨트롤(비활성 fieldset 의 영향 포함), 비활성 option 이나 optgroup.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean disabled(Element e) {
        if (e instanceof HtmlListedElement) {
            return of(((HtmlListedElement) e).isDisabled());
        } else if (e instanceof HtmlOptionElement) {
            return of(((HtmlOptionElement) e).isDisabled());
        } else if (e instanceof HtmlOptGroupElement) {
            return of(((HtmlOptGroupElement) e).isDisabled());
        }
        return null;
    }

    /**
     * <code>:enabled</code>: a form control that can be disabled and is not.
     * <p>
     * <code>:enabled</code>: 비활성이 될 수 있는데 아닌 폼 컨트롤.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean enabled(Element e) {
        if (e instanceof HtmlListedElement) {
            HtmlListedElement c = (HtmlListedElement) e;
            return of(c.canBeDisabled() && !c.isDisabled());
        } else if (e instanceof HtmlOptionElement || e instanceof HtmlOptGroupElement) {
            return of(!disabled(e).booleanValue());
        }
        return null;
    }

    /**
     * <code>:indeterminate</code>: a checkbox whose indeterminateness is true, a radio button whose group has no checked member, a progress
     * element without a <code>value</code>.
     * <p>
     * <code>:indeterminate</code>: indeterminateness 가 true 인 체크박스, 그룹에 체크된 구성원이 없는 라디오 버튼, <code>value</code> 가 없는 progress.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean indeterminate(Element e) {
        if (e instanceof HtmlInputElement) {
            HtmlInputElement in = (HtmlInputElement) e;
            if (in.kind() == InputKinds.CHECKBOX) {
                return of(in.isIndeterminate());
            }
            return of(in.kind() == InputKinds.RADIO && !in.groupHasChecked());
        } else if (e instanceof HtmlProgressElement) {
            return of(!((HtmlProgressElement) e).isDeterminate());
        }
        return null;
    }

    /**
     * <code>:default</code>: a checkbox or radio button with the <code>checked</code> attribute, an option with the <code>selected</code> attribute, and the default
     * button of a form (its first submit button in tree order).
     * <p>
     * <code>:default</code>: <code>checked</code> 속성이 있는 체크박스나 라디오 버튼, <code>selected</code> 속성이 있는 option, 폼의 기본 버튼(트리 순서로 첫 제출 버튼).
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean isDefault(Element e) {
        if (e instanceof HtmlInputElement) {
            HtmlInputElement in = (HtmlInputElement) e;
            if (in.kind() == InputKinds.CHECKBOX || in.kind() == InputKinds.RADIO) {
                return of(in.isDefaultChecked());
            }
            return of(in.isSubmitButton() && isDefaultButton(in, in.getForm()));
        } else if (e instanceof HtmlOptionElement) {
            return of(((HtmlOptionElement) e).isDefaultSelected());
        } else if (e instanceof HtmlButtonElement) {
            HtmlButtonElement b = (HtmlButtonElement) e;
            return of(b.isSubmitButton() && isDefaultButton(b, b.getForm()));
        }
        return null;
    }

    /**
     * Tells whether a control is the default button of a form.
     * <p>
     * 컨트롤이 폼의 기본 버튼인지 알려 준다.
     *
     * @param e    the control / 컨트롤
     * @param form the form owner or null / 폼 소유자 또는 null
     * @return true for the default button / 기본 버튼이면 true
     */
    private static boolean isDefaultButton(Element e, HtmlFormElement form) {
        return form != null && form.getDefaultButton() == e;
    }

    /**
     * <code>:required</code>: a control to which the <code>required</code> attribute applies and that has it.
     * <p>
     * <code>:required</code>: <code>required</code> 속성이 적용되고 그 속성이 있는 컨트롤.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean required(Element e) {
        Boolean applies = requiredApplies(e);
        return applies == null ? null : of(applies.booleanValue() && e.hasAttribute("required"));
    }

    /**
     * <code>:optional</code>: a control to which the <code>required</code> attribute applies and that does not have it.
     * <p>
     * <code>:optional</code>: <code>required</code> 속성이 적용되는데 그 속성이 없는 컨트롤.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean optional(Element e) {
        Boolean applies = requiredApplies(e);
        return applies == null ? null : of(applies.booleanValue() && !e.hasAttribute("required"));
    }

    /**
     * Tells whether the <code>required</code> attribute applies to an element.
     * <p>
     * <code>required</code> 속성이 요소에 적용되는지 알려 준다.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    private static Boolean requiredApplies(Element e) {
        if (e instanceof HtmlInputElement) {
            return of(InputKinds.requiredApplies(((HtmlInputElement) e).kind()));
        } else if (e instanceof HtmlSelectElement || e instanceof HtmlTextAreaElement) {
            return Boolean.TRUE;
        } else if (e instanceof HtmlControl) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * <code>:read-write</code>: a text control that the user can edit (not disabled, not read-only). <code>:read-only</code> is its negation.
     * <p>
     * <code>:read-write</code>: 사용자가 편집할 수 있는(비활성이 아니고 읽기 전용이 아닌) 텍스트 컨트롤. <code>:read-only</code> 는 그 부정이다.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not decide it for the element / 답. 폼 계층이 정하지 않는 요소이면 null
     */
    public static Boolean readWrite(Element e) {
        if (e instanceof HtmlInputElement) {
            HtmlInputElement in = (HtmlInputElement) e;
            return of(InputKinds.readonlyApplies(in.kind()) && in.isMutable());
        } else if (e instanceof HtmlTextAreaElement) {
            return of(((HtmlTextAreaElement) e).isMutable());
        } else if (e instanceof HtmlControl) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * <code>:placeholder-shown</code>: a text control whose placeholder is displayed (it has a non-empty placeholder and its value is empty).
     * <p>
     * <code>:placeholder-shown</code>: placeholder 가 표시되는(비어 있지 않은 placeholder 가 있고 값이 비어 있는) 텍스트 컨트롤.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean placeholderShown(Element e) {
        if (e instanceof HtmlInputElement) {
            return of(((HtmlInputElement) e).isPlaceholderShown());
        } else if (e instanceof HtmlTextAreaElement) {
            return of(((HtmlTextAreaElement) e).isPlaceholderShown());
        } else if (e instanceof HtmlControl) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * <code>:valid</code>: a control that is a candidate for constraint validation and satisfies its constraints; a form or fieldset whose candidate controls are all valid.
     * <p>
     * <code>:valid</code>: 제약 검증의 대상이며 제약을 만족하는 컨트롤. 대상 컨트롤이 모두 올바른 form 이나 fieldset.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean valid(Element e) {
        Boolean v = validity(e);
        return v == null ? null : of(v.booleanValue() && isCandidate(e));
    }

    /**
     * <code>:invalid</code>: a control that is a candidate for constraint validation and violates a constraint; a form or fieldset with an invalid candidate control.
     * <p>
     * <code>:invalid</code>: 제약 검증의 대상이며 제약을 어기는 컨트롤. 올바르지 않은 대상 컨트롤이 있는 form 이나 fieldset.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean invalid(Element e) {
        Boolean v = validity(e);
        return v == null ? null : of(!v.booleanValue() && isCandidate(e));
    }

    /**
     * Tells whether an element takes part in the pseudo-classes <code>:valid</code> and <code>:invalid</code> at all: a candidate for constraint validation, a form
     * or a fieldset.
     * <p>
     * 요소가 <code>:valid</code>, <code>:invalid</code> 에 아예 참여하는지 알려 준다: 제약 검증의 대상, form, fieldset.
     *
     * @param e the element / 요소
     * @return true if it takes part / 참여하면 true
     */
    private static boolean isCandidate(Element e) {
        return e instanceof HtmlFormElement || e instanceof HtmlFieldSetElement || (e instanceof HtmlListedElement && ((HtmlListedElement) e).willValidate());
    }

    /**
     * Computes whether the constraints of an element (or of the controls of a form or fieldset) are satisfied.
     * <p>
     * 요소(또는 form, fieldset 의 컨트롤)의 제약이 만족되는지 계산한다.
     *
     * @param e the element / 요소
     * @return TRUE if satisfied, FALSE if violated, null if the form layer does not implement the element / 만족하면 TRUE, 어기면 FALSE. 폼 계층이 구현하지 않는 요소이면 null
     */
    private static Boolean validity(Element e) {
        if (e instanceof HtmlFormElement) {
            List<HtmlListedElement> all = ((HtmlFormElement) e).listedElements();
            // 폼의 컨트롤 중 검증 대상이면서 제약을 어기는 것이 있는지 본다.
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).willValidate() && all.get(i).validityFlags() != 0) {
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        } else if (e instanceof HtmlFieldSetElement) {
            // fieldset 의 후손 컨트롤 중 검증 대상이면서 제약을 어기는 것이 있는지 본다.
            for (Node n = Node.nextInTree(e, e); n != null; n = Node.nextInTree(n, e)) {
                if (n instanceof HtmlListedElement && ((HtmlListedElement) n).willValidate() && ((HtmlListedElement) n).validityFlags() != 0) {
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        } else if (e instanceof HtmlListedElement) {
            return of(((HtmlListedElement) e).validityFlags() == 0);
        }
        return null;
    }

    /**
     * <code>:in-range</code>: an input that has range limitations and whose value is neither below the minimum nor above the maximum.
     * <p>
     * <code>:in-range</code>: 범위 제한이 있고 값이 최솟값보다 작지도 최댓값보다 크지도 않은 input.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean inRange(Element e) {
        if (e instanceof HtmlInputElement) {
            HtmlInputElement in = (HtmlInputElement) e;
            return of(in.hasRangeLimits() && in.willValidate() && !in.isOutOfRange());
        }
        return e instanceof HtmlControl ? Boolean.FALSE : null;
    }

    /**
     * <code>:out-of-range</code>: an input that has range limitations and whose value is below the minimum or above the maximum.
     * <p>
     * <code>:out-of-range</code>: 범위 제한이 있고 값이 최솟값보다 작거나 최댓값보다 큰 input.
     *
     * @param e the element / 요소
     * @return the answer, or null if the form layer does not implement the element / 답. 폼 계층이 구현하지 않는 요소이면 null
     */
    public static Boolean outOfRange(Element e) {
        if (e instanceof HtmlInputElement) {
            HtmlInputElement in = (HtmlInputElement) e;
            return of(in.hasRangeLimits() && in.willValidate() && in.isOutOfRange());
        }
        return e instanceof HtmlControl ? Boolean.FALSE : null;
    }
}
