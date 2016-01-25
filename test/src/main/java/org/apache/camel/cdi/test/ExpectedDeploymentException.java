package org.apache.camel.cdi.test;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

public final class ExpectedDeploymentException implements TestRule {

    private final List<Matcher<Throwable>> exceptions = new ArrayList<>();

    private final List<Matcher<String>> messages = new ArrayList<>();

    private final LogVerifier log = new LogVerifier();

    private final TestRule chain;

    private ExpectedDeploymentException() {
        chain = RuleChain
            .outerRule(log)
            .around(new TestRule() {
                @Override
                public Statement apply(final Statement base, Description description) {
                    return new Statement() {
                        @Override
                        public void evaluate() throws Throwable {
                            try {
                                base.evaluate();
                            } catch (Throwable exception) {
                                assertThat(exception, allOf(pecs(exceptions)));
                                try {
                                    // OpenWebBeans logs the deployment exception details
                                    // TODO: OpenWebBeans only log the root cause of exception thrown in producer methods
                                    //assertThat(log.getMessages(), containsInRelativeOrder(pecs(messages)))
                                    assertThat(log.getMessages(), anyOf(hasItems(messages)));
                                } catch (AssertionError error) {
                                    // Weld stores the deployment exception details in the exception message
                                    assertThat(exception.getMessage(), allOf(pecs(messages)));
                                }
                            }
                        }
                    };
                }
            });
    }

    public static ExpectedDeploymentException none() {
        return new ExpectedDeploymentException();
    }

    public ExpectedDeploymentException expect(Class<? extends Throwable> type) {
        exceptions.add(Matchers.instanceOf(type));
        return this;
    }

    public ExpectedDeploymentException expectMessage(Matcher<String> matcher) {
        messages.add(matcher);
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> List<Matcher<? super T>> pecs(List<Matcher<T>> matchers) {
        return new ArrayList<>((List) matchers);
    }

    private <T> Matcher<Iterable<? super T>>[] hasItems(List<Matcher<T>> matchers) {
        @SuppressWarnings("unchecked")
        Matcher<Iterable<? super T>>[] items = new Matcher[matchers.size()];
        for (int i = 0; i < items.length; i++)
            items[i] = hasItem(matchers.get(i));
        return items;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return chain.apply(base, description);
    }
}
