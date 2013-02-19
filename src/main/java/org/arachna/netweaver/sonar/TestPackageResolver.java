/**
 * 
 */
package org.arachna.netweaver.sonar;

import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;

/**
 * @author Dirk Weigenand
 */
public class TestPackageResolver extends VoidVisitorAdapter {

    @Override
    public void visit(final MethodDeclaration methodDeclaration, final Object arg) {
        for (final AnnotationExpr annotation : methodDeclaration.getAnnotations()) {
            final NameExpr nameExpr = annotation.getName();
            System.err.println(nameExpr.getName());
        }
    }
}
