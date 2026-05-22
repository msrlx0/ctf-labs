<?php
require_once __DIR__ . '/includes/layout.php';
$tickets = db()->query('SELECT * FROM support_tickets WHERE visibility = "public" ORDER BY id ASC')->fetchAll();
page_header('Support');
?>
<section class="panel">
  <p class="eyebrow">Support desk</p>
  <h1>Migration tickets</h1>
  <p class="muted">Buyer-visible tickets are summarized from operations notes. Older wording may mention workflow names, but support visibility is not the same as settlement approval.</p>
  <div class="status-row">
    <span class="badge good">Public queue</span>
    <span class="badge">Migration window</span>
    <span class="badge warn">Context-sensitive</span>
  </div>
  <div class="table-wrap">
    <table>
      <thead><tr><th>ID</th><th>Subject</th><th>Status</th><th>Note</th></tr></thead>
      <tbody>
      <?php foreach ($tickets as $ticket): ?>
        <tr>
          <td><?= (int)$ticket['id'] ?></td>
          <td><?= e($ticket['subject']) ?></td>
          <td><span class="badge"><?= e($ticket['status']) ?></span></td>
          <td><?= e($ticket['body']) ?></td>
        </tr>
      <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</section>
<?php page_footer(); ?>
